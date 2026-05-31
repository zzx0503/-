package com.bookstore.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bookstore.client.BookServiceClient;
import com.bookstore.domain.dto.seckill.SeckillActivityDTO;
import com.bookstore.domain.po.SeckillActivity;
import com.bookstore.domain.po.SeckillOrder;
import com.bookstore.domain.vo.book.BookDetailVO;
import com.bookstore.domain.vo.seckill.SeckillActivityVO;
import com.bookstore.exception.BusinessException;
import com.bookstore.mapper.SeckillActivityMapper;
import com.bookstore.mapper.SeckillOrderMapper;
import com.bookstore.response.PageResult;
import com.bookstore.response.Result;
import com.bookstore.response.ResultCode;
import com.bookstore.service.SeckillActivityService;
import com.bookstore.utils.OssUrlBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RedissonClient;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SeckillActivityServiceImpl implements SeckillActivityService {

    public static final String STATUS_READY = "READY";
    public static final String STATUS_RUNNING = "RUNNING";
    public static final String STATUS_ENDED = "ENDED";

    public static final String STOCK_KEY_PREFIX = "seckill:stock:";
    public static final String BOUGHT_KEY_PREFIX = "seckill:bought:";

    private final SeckillActivityMapper seckillActivityMapper;
    private final SeckillOrderMapper seckillOrderMapper;
    private final BookServiceClient bookServiceClient;
    private final RedissonClient redissonClient;
    private final OssUrlBuilder ossUrlBuilder;

    private BookDetailVO requireBook(Long bookId) {
        Result<BookDetailVO> result = bookServiceClient.getBook(bookId);
        if (result == null || result.getCode() != ResultCode.SUCCESS.getCode()) {
            throw new BusinessException(ResultCode.BOOK_NOT_FOUND);
        }
        BookDetailVO book = result.getData();
        if (book == null || book.getDeleted() == 1) {
            throw new BusinessException(ResultCode.BOOK_NOT_FOUND);
        }
        return book;
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = {"seckill:running", "seckill:upcoming"}, allEntries = true)
    public Long create(SeckillActivityDTO dto) {
        BookDetailVO book = requireBook(dto.getBookId());
        if (dto.getStartTime().isAfter(dto.getEndTime()) || dto.getStartTime().isEqual(dto.getEndTime())) {
            throw new BusinessException(ResultCode.PARAM_INVALID, "开始时间必须早于结束时间");
        }
        SeckillActivity a = new SeckillActivity();
        a.setBookId(dto.getBookId());
        a.setSeckillPrice(dto.getSeckillPrice());
        a.setOriginalPrice(book.getPrice());
        a.setTotalStock(dto.getTotalStock());
        a.setSoldCount(0);
        a.setPerUserLimit(dto.getPerUserLimit() == null ? 1 : dto.getPerUserLimit());
        a.setStartTime(dto.getStartTime());
        a.setEndTime(dto.getEndTime());
        a.setStatus(STATUS_READY);
        a.setTitle(StringUtils.hasText(dto.getTitle()) ? dto.getTitle() : book.getTitle());
        seckillActivityMapper.insert(a);
        return a.getId();
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = {"seckill:running", "seckill:upcoming"}, allEntries = true)
    public void update(Long id, SeckillActivityDTO dto) {
        SeckillActivity exist = requireById(id);
        if (STATUS_RUNNING.equals(exist.getStatus()) || STATUS_ENDED.equals(exist.getStatus())) {
            throw new BusinessException(ResultCode.BIZ_ERROR, "已开始或已结束的活动不可编辑");
        }
        seckillActivityMapper.update(null,
            new LambdaUpdateWrapper<SeckillActivity>()
                .eq(SeckillActivity::getId, id)
                .set(SeckillActivity::getSeckillPrice, dto.getSeckillPrice())
                .set(SeckillActivity::getTotalStock, dto.getTotalStock())
                .set(SeckillActivity::getPerUserLimit, dto.getPerUserLimit() == null ? 1 : dto.getPerUserLimit())
                .set(SeckillActivity::getStartTime, dto.getStartTime())
                .set(SeckillActivity::getEndTime, dto.getEndTime())
                .set(SeckillActivity::getTitle, dto.getTitle())
        );
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = {"seckill:running", "seckill:upcoming"}, allEntries = true)
    public void delete(Long id) {
        SeckillActivity exist = requireById(id);
        if (STATUS_RUNNING.equals(exist.getStatus())) {
            throw new BusinessException(ResultCode.BIZ_ERROR, "进行中的活动不可删除,请先结束");
        }
        seckillActivityMapper.deleteById(id);
        cleanupRedis(id);
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = {"seckill:running", "seckill:upcoming"}, allEntries = true)
    public void start(Long id) {
        SeckillActivity a = requireById(id);
        if (!STATUS_READY.equals(a.getStatus())) {
            throw new BusinessException(ResultCode.BIZ_ERROR, "只有 READY 状态的活动可以开始");
        }
        if (LocalDateTime.now().isAfter(a.getEndTime())) {
            throw new BusinessException(ResultCode.BIZ_ERROR, "活动已过期不可开始");
        }
        org.redisson.api.RAtomicLong stock = redissonClient.getAtomicLong(STOCK_KEY_PREFIX + id);
        stock.set(a.getTotalStock());
        long ttlSeconds = Math.max(60, Duration.between(LocalDateTime.now(), a.getEndTime()).plusHours(1).getSeconds());
        stock.expire(Duration.ofSeconds(ttlSeconds));
        seckillActivityMapper.update(null,
            new LambdaUpdateWrapper<SeckillActivity>()
                .eq(SeckillActivity::getId, id)
                .eq(SeckillActivity::getStatus, STATUS_READY)
                .set(SeckillActivity::getStatus, STATUS_RUNNING)
        );
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = {"seckill:running", "seckill:upcoming"}, allEntries = true)
    public void end(Long id) {
        SeckillActivity a = requireById(id);
        if (STATUS_ENDED.equals(a.getStatus())) {
            return;
        }
        seckillActivityMapper.update(null,
            new LambdaUpdateWrapper<SeckillActivity>()
                .eq(SeckillActivity::getId, id)
                .set(SeckillActivity::getStatus, STATUS_ENDED)
        );
        cleanupRedis(id);
    }

    @Override
    public SeckillActivityVO detail(Long id, Long userId) {
        SeckillActivity a = requireById(id);
        BookDetailVO book = requireBook(a.getBookId());
        SeckillActivityVO vo = toVO(a, book);
        if (userId != null) {
            vo.setUserStatus(resolveUserStatus(userId, a));
        }
        return vo;
    }

    @Override
    public PageResult<SeckillActivityVO> listAdmin(String status, Integer page, Integer size) {
        LambdaQueryWrapper<SeckillActivity> w = new LambdaQueryWrapper<SeckillActivity>()
            .eq(SeckillActivity::getDeleted, 0)
            .orderByDesc(SeckillActivity::getStartTime);
        if (StringUtils.hasText(status)) {
            w.eq(SeckillActivity::getStatus, status);
        }
        Page<SeckillActivity> p = seckillActivityMapper.selectPage(new Page<>(page, size), w);
        Map<Long, BookDetailVO> bookMap = loadBookMap(p.getRecords());
        List<SeckillActivityVO> list = p.getRecords().stream()
            .map(a -> toVO(a, bookMap.get(a.getBookId())))
            .collect(Collectors.toList());
        return PageResult.of(list, p.getTotal(), p.getCurrent(), p.getSize());
    }

    @Override
    @Cacheable(cacheNames = "seckill:running", key = "#userId != null ? #userId : 'all'")
    public List<SeckillActivityVO> listRunning(Long userId) {
        LocalDateTime now = LocalDateTime.now();
        List<SeckillActivity> activities = seckillActivityMapper.selectList(
            new LambdaQueryWrapper<SeckillActivity>()
                .eq(SeckillActivity::getStatus, STATUS_RUNNING)
                .le(SeckillActivity::getStartTime, now)
                .ge(SeckillActivity::getEndTime, now)
                .eq(SeckillActivity::getDeleted, 0)
                .orderByAsc(SeckillActivity::getEndTime)
        );
        return enrich(activities, userId);
    }

    @Override
    @Cacheable(cacheNames = "seckill:upcoming", key = "#userId != null ? #userId : 'all'")
    public List<SeckillActivityVO> listUpcoming(Long userId) {
        LocalDateTime now = LocalDateTime.now();
        List<SeckillActivity> activities = seckillActivityMapper.selectList(
            new LambdaQueryWrapper<SeckillActivity>()
                .in(SeckillActivity::getStatus, List.of(STATUS_READY, STATUS_RUNNING))
                .gt(SeckillActivity::getStartTime, now)
                .eq(SeckillActivity::getDeleted, 0)
                .orderByAsc(SeckillActivity::getStartTime)
        );
        return enrich(activities, userId);
    }

    @Override
    public SeckillActivity requireById(Long id) {
        SeckillActivity a = seckillActivityMapper.selectById(id);
        if (a == null || (a.getDeleted() != null && a.getDeleted() == 1)) {
            throw new BusinessException(ResultCode.NOT_FOUND, "秒杀活动不存在");
        }
        return a;
    }

    @Scheduled(cron = "30 * * * * ?")
    public void autoEndExpired() {
        LocalDateTime now = LocalDateTime.now();
        List<SeckillActivity> expired = seckillActivityMapper.selectList(
            new LambdaQueryWrapper<SeckillActivity>()
                .eq(SeckillActivity::getStatus, STATUS_RUNNING)
                .lt(SeckillActivity::getEndTime, now)
                .eq(SeckillActivity::getDeleted, 0)
        );
        for (SeckillActivity a : expired) {
            try {
                end(a.getId());
            } catch (Exception e) {
                log.warn("auto end seckill activity failed, id={}", a.getId(), e);
            }
        }
        if (!expired.isEmpty()) {
            log.info("auto ended {} seckill activities", expired.size());
        }
    }

    private void cleanupRedis(Long id) {
        try {
            redissonClient.getAtomicLong(STOCK_KEY_PREFIX + id).delete();
            redissonClient.getSet(BOUGHT_KEY_PREFIX + id).delete();
        } catch (Exception e) {
            log.warn("cleanup seckill redis failed, id={}", id, e);
        }
    }

    private List<SeckillActivityVO> enrich(List<SeckillActivity> activities, Long userId) {
        if (activities.isEmpty()) {
            return List.of();
        }
        Map<Long, BookDetailVO> bookMap = loadBookMap(activities);
        return activities.stream().map(a -> {
            SeckillActivityVO vo = toVO(a, bookMap.get(a.getBookId()));
            if (userId != null) {
                vo.setUserStatus(resolveUserStatus(userId, a));
            }
            return vo;
        }).collect(Collectors.toList());
    }

    private Map<Long, BookDetailVO> loadBookMap(List<SeckillActivity> activities) {
        if (activities.isEmpty()) {
            return new HashMap<>();
        }
        Set<Long> bookIds = activities.stream().map(SeckillActivity::getBookId).collect(Collectors.toCollection(HashSet::new));
        Map<Long, BookDetailVO> map = new HashMap<>();
        for (Long id : bookIds) {
            Result<BookDetailVO> result = bookServiceClient.getBook(id);
            if (result != null && result.getCode() == ResultCode.SUCCESS.getCode() && result.getData() != null) {
                map.put(id, result.getData());
            }
        }
        return map;
    }

    private SeckillActivityVO toVO(SeckillActivity a, BookDetailVO book) {
        SeckillActivityVO vo = new SeckillActivityVO();
        vo.setId(a.getId());
        vo.setBookId(a.getBookId());
        vo.setSeckillPrice(a.getSeckillPrice());
        vo.setOriginalPrice(a.getOriginalPrice());
        vo.setTotalStock(a.getTotalStock());
        vo.setSoldCount(a.getSoldCount());
        vo.setPerUserLimit(a.getPerUserLimit());
        vo.setStartTime(a.getStartTime());
        vo.setEndTime(a.getEndTime());
        vo.setStatus(a.getStatus());
        vo.setTitle(a.getTitle());
        vo.setCreateTime(a.getCreateTime());
        if (book != null) {
            vo.setBookTitle(book.getTitle());
            vo.setBookCover(ossUrlBuilder.toFullUrl(book.getCoverKey()));
            vo.setBookAuthor(book.getAuthor());
        }
        int dbRemaining = Math.max(0, a.getTotalStock() - (a.getSoldCount() == null ? 0 : a.getSoldCount()));
        if (STATUS_RUNNING.equals(a.getStatus())) {
            try {
                org.redisson.api.RAtomicLong stock = redissonClient.getAtomicLong(STOCK_KEY_PREFIX + a.getId());
                long redisRemaining = stock.get();
                if (redisRemaining == 0 && dbRemaining > 0) {
                    stock.set(dbRemaining);
                    long ttlSeconds = Math.max(60, java.time.Duration.between(LocalDateTime.now(), a.getEndTime()).plusHours(1).getSeconds());
                    stock.expire(java.time.Duration.ofSeconds(ttlSeconds));
                    vo.setRemainingStock(dbRemaining);
                } else {
                    vo.setRemainingStock((int) redisRemaining);
                }
            } catch (Exception e) {
                vo.setRemainingStock(dbRemaining);
            }
        } else {
            vo.setRemainingStock(dbRemaining);
        }
        return vo;
    }

    private String resolveUserStatus(Long userId, SeckillActivity a) {
        SeckillOrder existing = seckillOrderMapper.selectOne(
            new LambdaQueryWrapper<SeckillOrder>()
                .eq(SeckillOrder::getActivityId, a.getId())
                .eq(SeckillOrder::getUserId, userId)
                .eq(SeckillOrder::getDeleted, 0)
                .orderByDesc(SeckillOrder::getCreateTime)
                .last("LIMIT 1")
        );
        if (existing == null) {
            return "NONE";
        }
        return existing.getStatus();
    }
}
