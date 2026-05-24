package com.bookstore.service.impl;

import com.bookstore.domain.dto.seckill.SeckillBuyDTO;
import com.bookstore.domain.dto.seckill.SeckillQueueRequestDTO;
import com.bookstore.domain.po.Address;
import com.bookstore.domain.po.SeckillActivity;
import com.bookstore.domain.vo.seckill.SeckillBuyResultVO;
import com.bookstore.domain.vo.seckill.SeckillQueueStatusVO;
import com.bookstore.exception.BusinessException;
import com.bookstore.mapper.AddressMapper;
import com.bookstore.mapper.SeckillActivityMapper;
import com.bookstore.response.ResultCode;
import com.bookstore.service.SeckillQueueService;
import com.bookstore.service.SeckillService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RScript;
import org.redisson.client.codec.LongCodec;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class SeckillQueueServiceImpl implements SeckillQueueService {

    private static final String QUEUE_KEY = "seckill:queue:orders";
    private static final String STATUS_KEY_PREFIX = "seckill:queue:status:";
    private static final long STATUS_TTL_MINUTES = 10;
    private static final int MAX_QUEUE_SIZE = 10000;

    private static final String SECKILL_LUA =
        "local stockKey = KEYS[1]\n" +
        "local boughtKey = KEYS[2]\n" +
        "local userId = ARGV[1]\n" +
        "local limit = tonumber(ARGV[2])\n" +
        "local bought = tonumber(redis.call('HGET', boughtKey, userId) or '0')\n" +
        "if bought >= limit then\n" +
        "    return -1\n" +
        "end\n" +
        "local stock = redis.call('GET', stockKey)\n" +
        "if not stock then\n" +
        "    return -2\n" +
        "end\n" +
        "if tonumber(stock) <= 0 then\n" +
        "    return 0\n" +
        "end\n" +
        "redis.call('DECR', stockKey)\n" +
        "redis.call('HINCRBY', boughtKey, userId, 1)\n" +
        "return 1";

    private static final String ROLLBACK_LUA =
        "redis.call('INCR', KEYS[1])\n" +
        "redis.call('HINCRBY', KEYS[2], ARGV[1], -1)\n" +
        "return 1";

    private final RedissonClient redissonClient;
    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;
    private final SeckillService seckillService;
    private final SeckillActivityMapper seckillActivityMapper;
    private final AddressMapper addressMapper;

    @Override
    public String enqueue(Long userId, SeckillBuyDTO dto) {
        SeckillActivity activity = seckillActivityMapper.selectById(dto.getActivityId());
        if (activity == null || Boolean.TRUE.equals(activity.getDeleted())) {
            throw new BusinessException(ResultCode.NOT_FOUND, "秒杀活动不存在");
        }
        if (!SeckillActivityServiceImpl.STATUS_RUNNING.equals(activity.getStatus())) {
            throw new BusinessException(ResultCode.SECKILL_NOT_RUNNING);
        }
        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(activity.getStartTime()) || now.isAfter(activity.getEndTime())) {
            throw new BusinessException(ResultCode.SECKILL_NOT_RUNNING);
        }

        Address address = addressMapper.selectById(dto.getAddressId());
        if (address == null || Boolean.TRUE.equals(address.getDeleted())) {
            throw new BusinessException(ResultCode.NOT_FOUND, "收货地址不存在");
        }
        if (!address.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.FORBIDDEN);
        }

        Long queueSize = stringRedisTemplate.opsForList().size(QUEUE_KEY);
        if (queueSize != null && queueSize >= MAX_QUEUE_SIZE) {
            throw new BusinessException(ResultCode.SECKILL_QUEUE_FULL);
        }

        String stockKey = SeckillActivityServiceImpl.STOCK_KEY_PREFIX + activity.getId();
        String boughtKey = SeckillActivityServiceImpl.BOUGHT_KEY_PREFIX + activity.getId();

        Long luaResult = redissonClient.getScript(LongCodec.INSTANCE).eval(
            RScript.Mode.READ_WRITE,
            SECKILL_LUA,
            RScript.ReturnType.INTEGER,
            Arrays.asList(stockKey, boughtKey),
            String.valueOf(userId),
            String.valueOf(activity.getPerUserLimit() == null ? 1 : activity.getPerUserLimit())
        );

        if (luaResult == null) {
            throw new BusinessException(ResultCode.SECKILL_NOT_RUNNING);
        }
        if (luaResult == -1L) {
            throw new BusinessException(ResultCode.SECKILL_LIMIT_REACHED);
        }
        if (luaResult == -2L) {
            throw new BusinessException(ResultCode.SECKILL_NOT_RUNNING, "活动未启动或已结束");
        }
        if (luaResult == 0L) {
            throw new BusinessException(ResultCode.SECKILL_OUT_OF_STOCK);
        }

        String requestId = generateRequestId();
        SeckillQueueRequestDTO req = SeckillQueueRequestDTO.builder()
            .requestId(requestId)
            .userId(userId)
            .activityId(dto.getActivityId())
            .addressId(dto.getAddressId())
            .timestamp(now)
            .build();

        stringRedisTemplate.opsForList().leftPush(QUEUE_KEY, toJson(req));

        SeckillQueueStatusVO status = new SeckillQueueStatusVO();
        status.setRequestId(requestId);
        status.setStatus("PROCESSING");
        stringRedisTemplate.opsForValue().set(
            STATUS_KEY_PREFIX + requestId,
            toJson(status),
            Duration.ofMinutes(STATUS_TTL_MINUTES)
        );

        return requestId;
    }

    @Override
    public SeckillQueueStatusVO queryStatus(String requestId) {
        String json = stringRedisTemplate.opsForValue().get(STATUS_KEY_PREFIX + requestId);
        if (json == null) {
            SeckillQueueStatusVO vo = new SeckillQueueStatusVO();
            vo.setRequestId(requestId);
            vo.setStatus("FAILED");
            vo.setMsg("请求已过期或不存在");
            return vo;
        }
        return fromJson(json, SeckillQueueStatusVO.class);
    }

    @Override
    public void consume() {
        String json;
        while ((json = stringRedisTemplate.opsForList().rightPop(QUEUE_KEY)) != null) {
            SeckillQueueRequestDTO req = fromJson(json, SeckillQueueRequestDTO.class);
            if (req == null) continue;
            processOne(req);
        }
    }

    private void processOne(SeckillQueueRequestDTO req) {
        try {
            SeckillBuyResultVO result = seckillService.createOrderFromQueue(
                req.getUserId(), req.getActivityId(), req.getAddressId()
            );

            SeckillQueueStatusVO status = new SeckillQueueStatusVO();
            status.setRequestId(req.getRequestId());
            status.setStatus("SUCCESS");
            status.setOrderNo(result.getOrderNo());
            status.setSeckillOrderId(result.getSeckillOrderId());
            status.setExpireSeconds(result.getExpireSeconds());
            stringRedisTemplate.opsForValue().set(
                STATUS_KEY_PREFIX + req.getRequestId(),
                toJson(status),
                Duration.ofMinutes(STATUS_TTL_MINUTES)
            );
            log.info("Queue order created, requestId={}, orderNo={}", req.getRequestId(), result.getOrderNo());
        } catch (Exception e) {
            log.error("Queue order creation failed, requestId={}", req.getRequestId(), e);
            rollbackRedis(req.getActivityId(), req.getUserId());

            SeckillQueueStatusVO status = new SeckillQueueStatusVO();
            status.setRequestId(req.getRequestId());
            status.setStatus("FAILED");
            status.setMsg(e.getMessage());
            stringRedisTemplate.opsForValue().set(
                STATUS_KEY_PREFIX + req.getRequestId(),
                toJson(status),
                Duration.ofMinutes(STATUS_TTL_MINUTES)
            );
        }
    }

    private void rollbackRedis(Long activityId, Long userId) {
        try {
            String stockKey = SeckillActivityServiceImpl.STOCK_KEY_PREFIX + activityId;
            String boughtKey = SeckillActivityServiceImpl.BOUGHT_KEY_PREFIX + activityId;
            redissonClient.getScript(LongCodec.INSTANCE).eval(
                RScript.Mode.READ_WRITE,
                ROLLBACK_LUA,
                RScript.ReturnType.INTEGER,
                Arrays.asList(stockKey, boughtKey),
                String.valueOf(userId)
            );
        } catch (Exception e) {
            log.warn("Rollback redis stock failed, activityId={}, userId={}", activityId, userId, e);
        }
    }

    private String generateRequestId() {
        String prefix = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        return "SKQ" + prefix + String.format("%06d", (int) (Math.random() * 1_000_000));
    }

    @SneakyThrows
    private String toJson(Object obj) {
        return objectMapper.writeValueAsString(obj);
    }

    @SneakyThrows
    private <T> T fromJson(String json, Class<T> clazz) {
        return objectMapper.readValue(json, clazz);
    }
}
