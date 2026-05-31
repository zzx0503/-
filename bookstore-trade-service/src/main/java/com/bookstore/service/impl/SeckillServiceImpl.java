package com.bookstore.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bookstore.api.book.client.BookClient;
import com.bookstore.api.user.client.UserClient;
import com.bookstore.domain.dto.seckill.SeckillBuyDTO;
import com.bookstore.domain.po.SeckillActivity;
import com.bookstore.domain.po.SeckillOrder;
import com.bookstore.api.user.dto.AddressDTO;
import com.bookstore.api.book.dto.BookDetailDTO;
import com.bookstore.domain.vo.seckill.SeckillBuyResultVO;
import com.bookstore.domain.vo.seckill.SeckillOrderVO;
import com.bookstore.exception.BusinessException;
import com.bookstore.mapper.SeckillActivityMapper;
import com.bookstore.mapper.SeckillOrderMapper;
import com.bookstore.response.PageResult;
import com.bookstore.response.Result;
import com.bookstore.response.ResultCode;
import com.bookstore.service.SeckillService;
import com.bookstore.utils.OssUrlBuilder;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RAtomicLong;
import org.redisson.api.RScript;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.LongCodec;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SeckillServiceImpl implements SeckillService {

    public static final String STATUS_PENDING = "PENDING_PAY";
    public static final String STATUS_PAID = "PAID";
    public static final String STATUS_CANCELLED = "CANCELLED";
    public static final String STATUS_EXPIRED = "EXPIRED";

    private static final long ORDER_EXPIRE_MINUTES = 5;

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

    private static final String DECR_BOUGHT_LUA =
        "return redis.call('HINCRBY', KEYS[1], ARGV[1], -1)";

    private final SeckillActivityMapper seckillActivityMapper;
    private final SeckillOrderMapper seckillOrderMapper;
    private final RedissonClient redissonClient;
    private final ObjectMapper objectMapper;
    private final OssUrlBuilder ossUrlBuilder;
    private final BookClient bookServiceClient;
    private final UserClient userServiceClient;

    private <T> T unwrap(Result<T> result) {
        if (result == null || result.getCode() != ResultCode.SUCCESS.getCode()) {
            String msg = result != null && result.getMsg() != null ? result.getMsg() : ResultCode.BIZ_ERROR.getDefaultMsg();
            throw new BusinessException(ResultCode.BIZ_ERROR, msg);
        }
        return result.getData();
    }

    @Override
    @Transactional
    public SeckillBuyResultVO buy(Long userId, SeckillBuyDTO dto) {
        SeckillActivity activity = validateActivity(dto.getActivityId());
        AddressDTO address = validateAddress(userId, dto.getAddressId());

        String stockKey = SeckillActivityServiceImpl.STOCK_KEY_PREFIX + activity.getId();
        String boughtKey = SeckillActivityServiceImpl.BOUGHT_KEY_PREFIX + activity.getId();

        Long result = redissonClient.getScript(LongCodec.INSTANCE).eval(
            RScript.Mode.READ_WRITE,
            SECKILL_LUA,
            RScript.ReturnType.INTEGER,
            Arrays.asList(stockKey, boughtKey),
            String.valueOf(userId),
            String.valueOf(activity.getPerUserLimit() == null ? 1 : activity.getPerUserLimit())
        );

        if (result != null && (result == -2L || result == 0L)) {
            int dbRemaining = Math.max(0, activity.getTotalStock() - (activity.getSoldCount() == null ? 0 : activity.getSoldCount()));
            if (dbRemaining > 0) {
                org.redisson.api.RAtomicLong stock = redissonClient.getAtomicLong(stockKey);
                stock.set(dbRemaining);
                long ttlSeconds = Math.max(60, java.time.Duration.between(LocalDateTime.now(), activity.getEndTime()).plusHours(1).getSeconds());
                stock.expire(java.time.Duration.ofSeconds(ttlSeconds));
                result = redissonClient.getScript(LongCodec.INSTANCE).eval(
                    RScript.Mode.READ_WRITE,
                    SECKILL_LUA,
                    RScript.ReturnType.INTEGER,
                    Arrays.asList(stockKey, boughtKey),
                    String.valueOf(userId),
                    String.valueOf(activity.getPerUserLimit() == null ? 1 : activity.getPerUserLimit())
                );
            }
        }

        if (result == null) {
            throw new BusinessException(ResultCode.SECKILL_NOT_RUNNING);
        }
        if (result == -1L) {
            throw new BusinessException(ResultCode.SECKILL_LIMIT_REACHED);
        }
        if (result == -2L) {
            throw new BusinessException(ResultCode.SECKILL_NOT_RUNNING, "活动未启动或已结束");
        }
        if (result == 0L) {
            throw new BusinessException(ResultCode.SECKILL_OUT_OF_STOCK);
        }

        try {
            return doCreateOrder(userId, activity, address);
        } catch (RuntimeException e) {
            try {
                redissonClient.getAtomicLong(stockKey).incrementAndGet();
                decrementBought(boughtKey, userId);
            } catch (Exception ignore) {
            }
            throw e;
        }
    }

    @Override
    @Transactional
    public SeckillBuyResultVO createOrderFromQueue(Long userId, Long activityId, Long addressId) {
        SeckillActivity activity = validateActivity(activityId);
        AddressDTO address = validateAddress(userId, addressId);
        return doCreateOrder(userId, activity, address);
    }

    private SeckillActivity validateActivity(Long activityId) {
        SeckillActivity activity = seckillActivityMapper.selectById(activityId);
        if (activity == null || (activity.getDeleted() != null && activity.getDeleted() == 1)) {
            throw new BusinessException(ResultCode.NOT_FOUND, "秒杀活动不存在");
        }
        if (!SeckillActivityServiceImpl.STATUS_RUNNING.equals(activity.getStatus())) {
            throw new BusinessException(ResultCode.SECKILL_NOT_RUNNING);
        }
        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(activity.getStartTime()) || now.isAfter(activity.getEndTime())) {
            throw new BusinessException(ResultCode.SECKILL_NOT_RUNNING);
        }
        return activity;
    }

    private AddressDTO validateAddress(Long userId, Long addressId) {
        AddressDTO address = unwrap(userServiceClient.getAddress(addressId));
        if (address == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "收货地址不存在");
        }
        // 内部接口返回的数据不带userId，无法直接校验；由调用方保证传入当前用户地址
        return address;
    }

    private SeckillBuyResultVO doCreateOrder(Long userId, SeckillActivity activity, AddressDTO address) {
        BookDetailDTO book = unwrap(bookServiceClient.getBook(activity.getBookId()));
        if (book == null || book.getDeleted() == 1 || book.getStatus() != 1) {
            throw new BusinessException(ResultCode.BOOK_NOT_FOUND);
        }
        Result<Void> deductResult = bookServiceClient.deductStock(book.getId(), 1);
        if (deductResult == null || deductResult.getCode() != ResultCode.SUCCESS.getCode()) {
            throw new BusinessException(ResultCode.STOCK_INSUFFICIENT, "图书库存不足");
        }

        String orderNo = generateOrderNo();
        LocalDateTime expireTime = LocalDateTime.now().plusMinutes(ORDER_EXPIRE_MINUTES);

        SeckillOrder order = new SeckillOrder();
        order.setOrderNo(orderNo);
        order.setUserId(userId);
        order.setActivityId(activity.getId());
        order.setBookId(activity.getBookId());
        order.setQuantity(1);
        order.setSeckillPrice(activity.getSeckillPrice());
        order.setTotalAmount(activity.getSeckillPrice());
        order.setStatus(STATUS_PENDING);
        order.setExpireTime(expireTime);
        order.setAddressSnapshot(toJsonSnapshot(address));
        seckillOrderMapper.insert(order);

        seckillActivityMapper.update(null,
            new LambdaUpdateWrapper<SeckillActivity>()
                .eq(SeckillActivity::getId, activity.getId())
                .setSql("sold_count = sold_count + 1")
        );

        SeckillBuyResultVO vo = new SeckillBuyResultVO();
        vo.setOrderNo(orderNo);
        vo.setSeckillOrderId(order.getId());
        vo.setExpireSeconds(ORDER_EXPIRE_MINUTES * 60);
        return vo;
    }

    @Override
    @Transactional
    public void payOrder(Long userId, String orderNo) {
        SeckillOrder order = requireOwnOrder(userId, orderNo);
        if (!STATUS_PENDING.equals(order.getStatus())) {
            throw new BusinessException(ResultCode.ORDER_STATUS_INVALID);
        }
        if (LocalDateTime.now().isAfter(order.getExpireTime())) {
            throw new BusinessException(ResultCode.SECKILL_ORDER_EXPIRED);
        }
        unwrap(userServiceClient.pay(userId, orderNo, order.getTotalAmount()));
        int affected = seckillOrderMapper.update(null,
            new LambdaUpdateWrapper<SeckillOrder>()
                .eq(SeckillOrder::getId, order.getId())
                .eq(SeckillOrder::getStatus, STATUS_PENDING)
                .set(SeckillOrder::getStatus, STATUS_PAID)
                .set(SeckillOrder::getPayTime, LocalDateTime.now())
        );
        if (affected == 0) {
            throw new BusinessException(ResultCode.ORDER_STATUS_INVALID);
        }
    }

    @Override
    @Transactional
    public void cancelOrder(Long userId, String orderNo) {
        SeckillOrder order = requireOwnOrder(userId, orderNo);
        if (!STATUS_PENDING.equals(order.getStatus())) {
            throw new BusinessException(ResultCode.ORDER_STATUS_INVALID, "只有待支付订单可以取消");
        }
        rollback(order, STATUS_CANCELLED);
    }

    @Override
    public SeckillOrderVO detail(Long userId, String orderNo) {
        SeckillOrder order = requireOwnOrder(userId, orderNo);
        BookDetailDTO book = unwrap(bookServiceClient.getBook(order.getBookId()));
        return toVO(order, book);
    }

    @Override
    public PageResult<SeckillOrderVO> listMyOrders(Long userId, String status, Integer page, Integer size) {
        LambdaQueryWrapper<SeckillOrder> w = new LambdaQueryWrapper<SeckillOrder>()
            .eq(SeckillOrder::getUserId, userId)
            .eq(SeckillOrder::getDeleted, 0)
            .orderByDesc(SeckillOrder::getCreateTime);
        if (StringUtils.hasText(status)) {
            w.eq(SeckillOrder::getStatus, status);
        }
        Page<SeckillOrder> p = seckillOrderMapper.selectPage(new Page<>(page, size), w);
        Map<Long, BookDetailDTO> bookMap = loadBookMap(p.getRecords());
        List<SeckillOrderVO> list = p.getRecords().stream()
            .map(o -> toVO(o, bookMap.get(o.getBookId())))
            .collect(Collectors.toList());
        return PageResult.of(list, p.getTotal(), p.getCurrent(), p.getSize());
    }

    @Override
    @org.springframework.scheduling.annotation.Scheduled(cron = "*/30 * * * * ?")
    public int autoExpire() {
        LocalDateTime now = LocalDateTime.now();
        List<SeckillOrder> expired = seckillOrderMapper.selectList(
            new LambdaQueryWrapper<SeckillOrder>()
                .eq(SeckillOrder::getStatus, STATUS_PENDING)
                .lt(SeckillOrder::getExpireTime, now)
                .eq(SeckillOrder::getDeleted, 0)
        );
        int count = 0;
        for (SeckillOrder o : expired) {
            try {
                rollback(o, STATUS_EXPIRED);
                count++;
            } catch (Exception e) {
                log.warn("auto expire seckill order failed, orderNo={}", o.getOrderNo(), e);
            }
        }
        if (count > 0) {
            log.info("auto expired {} seckill orders", count);
        }
        return count;
    }

    private void rollback(SeckillOrder order, String targetStatus) {
        int affected = seckillOrderMapper.update(null,
            new LambdaUpdateWrapper<SeckillOrder>()
                .eq(SeckillOrder::getId, order.getId())
                .eq(SeckillOrder::getStatus, STATUS_PENDING)
                .set(SeckillOrder::getStatus, targetStatus)
        );
        if (affected == 0) {
            return;
        }
        bookServiceClient.restoreStock(order.getBookId(), order.getQuantity());
        seckillActivityMapper.update(null,
            new LambdaUpdateWrapper<SeckillActivity>()
                .eq(SeckillActivity::getId, order.getActivityId())
                .gt(SeckillActivity::getSoldCount, 0)
                .setSql("sold_count = sold_count - " + order.getQuantity())
        );
        try {
            String stockKey = SeckillActivityServiceImpl.STOCK_KEY_PREFIX + order.getActivityId();
            String boughtKey = SeckillActivityServiceImpl.BOUGHT_KEY_PREFIX + order.getActivityId();
            org.redisson.api.RAtomicLong stockAtomic = redissonClient.getAtomicLong(stockKey);
            if (stockAtomic.isExists()) {
                stockAtomic.incrementAndGet();
            }
            decrementBought(boughtKey, order.getUserId());
        } catch (Exception e) {
            log.warn("rollback seckill redis state failed, orderNo={}", order.getOrderNo(), e);
        }
    }

    private void decrementBought(String boughtKey, Long userId) {
        try {
            redissonClient.getScript(LongCodec.INSTANCE).eval(
                RScript.Mode.READ_WRITE,
                DECR_BOUGHT_LUA,
                RScript.ReturnType.INTEGER,
                Arrays.asList(boughtKey),
                String.valueOf(userId)
            );
        } catch (Exception ignore) {
        }
    }

    private SeckillOrder requireOwnOrder(Long userId, String orderNo) {
        SeckillOrder order = seckillOrderMapper.selectOne(
            new LambdaQueryWrapper<SeckillOrder>().eq(SeckillOrder::getOrderNo, orderNo)
        );
        if (order == null || (order.getDeleted() != null && order.getDeleted() == 1)) {
            throw new BusinessException(ResultCode.SECKILL_ORDER_NOT_FOUND);
        }
        if (!order.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.FORBIDDEN);
        }
        return order;
    }

    private String generateOrderNo() {
        String datePrefix = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        RAtomicLong counter = redissonClient.getAtomicLong("seckill:order:seq:" + datePrefix.substring(0, 8));
        if (!counter.isExists()) {
            counter.expire(java.time.Duration.ofDays(2));
        }
        long seq = counter.incrementAndGet();
        return "SK" + datePrefix + String.format("%06d", seq);
    }

    @SneakyThrows
    private String toJsonSnapshot(AddressDTO address) {
        Map<String, Object> map = Map.of(
            "receiver", address.getReceiver(),
            "phone", address.getPhone(),
            "province", address.getProvince(),
            "city", address.getCity(),
            "district", address.getDistrict(),
            "detailAddress", address.getDetailAddress()
        );
        return objectMapper.writeValueAsString(map);
    }

    private Map<Long, BookDetailDTO> loadBookMap(List<SeckillOrder> orders) {
        if (orders.isEmpty()) {
            return new HashMap<>();
        }
        Set<Long> bookIds = orders.stream().map(SeckillOrder::getBookId).collect(Collectors.toCollection(HashSet::new));
        Map<Long, BookDetailDTO> map = new HashMap<>();
        for (Long id : bookIds) {
            Result<BookDetailDTO> result = bookServiceClient.getBook(id);
            if (result != null && result.getCode() == ResultCode.SUCCESS.getCode() && result.getData() != null) {
                map.put(id, result.getData());
            }
        }
        return map;
    }

    private SeckillOrderVO toVO(SeckillOrder o, BookDetailDTO book) {
        SeckillOrderVO vo = new SeckillOrderVO();
        vo.setId(o.getId());
        vo.setOrderNo(o.getOrderNo());
        vo.setActivityId(o.getActivityId());
        vo.setBookId(o.getBookId());
        vo.setQuantity(o.getQuantity());
        vo.setSeckillPrice(o.getSeckillPrice());
        vo.setTotalAmount(o.getTotalAmount());
        vo.setStatus(o.getStatus());
        vo.setPayTime(o.getPayTime());
        vo.setExpireTime(o.getExpireTime());
        vo.setCreateTime(o.getCreateTime());
        if (book != null) {
            vo.setBookTitle(book.getTitle());
            vo.setBookCover(ossUrlBuilder.toFullUrl(book.getCoverKey()));
        }
        return vo;
    }
}
