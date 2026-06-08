package com.bookstore.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bookstore.domain.dto.coupon.CouponTemplateDTO;
import com.bookstore.domain.po.CouponTemplate;
import com.bookstore.domain.po.UserCoupon;
import com.bookstore.domain.vo.coupon.AvailableCouponVO;
import com.bookstore.domain.vo.coupon.CouponTemplateVO;
import com.bookstore.exception.BusinessException;
import com.bookstore.mapper.CouponTemplateMapper;
import com.bookstore.mapper.UserCouponMapper;
import com.bookstore.response.PageResult;
import com.bookstore.response.ResultCode;
import com.bookstore.service.CouponTemplateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RSemaphore;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CouponTemplateServiceImpl implements CouponTemplateService {

    public static final String STATUS_READY = "READY";
    public static final String STATUS_RUNNING = "RUNNING";
    public static final String STATUS_ENDED = "ENDED";

    public static final String TYPE_FULL_REDUCE = "FULL_REDUCE";
    public static final String TYPE_DISCOUNT = "DISCOUNT";
    public static final String TYPE_AMOUNT = "AMOUNT";

    public static final String STOCK_KEY_PREFIX = "coupon:stock:";

    private final CouponTemplateMapper couponTemplateMapper;
    private final UserCouponMapper userCouponMapper;
    private final RedissonClient redissonClient;

    @Override
    @Transactional
    public Long create(CouponTemplateDTO dto) {
        validateType(dto.getType());
        if (dto.getValidFrom().isAfter(dto.getValidTo())) {
            throw new BusinessException(ResultCode.PARAM_INVALID, "生效时间必须早于失效时间");
        }
        if (dto.getTotalCount() == null || dto.getTotalCount() <= 0) {
            throw new BusinessException(ResultCode.PARAM_INVALID, "发放总量必须大于0");
        }
        CouponTemplate t = new CouponTemplate();
        t.setName(dto.getName());
        t.setType(dto.getType());
        t.setThreshold(dto.getThreshold());
        t.setDiscountValue(dto.getDiscountValue());
        t.setTotalCount(dto.getTotalCount());
        t.setClaimedCount(0);
        t.setValidFrom(dto.getValidFrom());
        t.setValidTo(dto.getValidTo());
        t.setStatus(STATUS_READY);
        t.setDescription(dto.getDescription());
        couponTemplateMapper.insert(t);
        return t.getId();
    }

    @Override
    @Transactional
    public void update(Long id, CouponTemplateDTO dto) {
        CouponTemplate exist = requireById(id);
        if (STATUS_RUNNING.equals(exist.getStatus()) || STATUS_ENDED.equals(exist.getStatus())) {
            throw new BusinessException(ResultCode.BIZ_ERROR, "已发布或已结束的优惠券不可编辑");
        }
        validateType(dto.getType());
        if (dto.getValidFrom().isAfter(dto.getValidTo())) {
            throw new BusinessException(ResultCode.PARAM_INVALID, "生效时间必须早于失效时间");
        }
        couponTemplateMapper.update(null,
            new LambdaUpdateWrapper<CouponTemplate>()
                .eq(CouponTemplate::getId, id)
                .set(CouponTemplate::getName, dto.getName())
                .set(CouponTemplate::getType, dto.getType())
                .set(CouponTemplate::getThreshold, dto.getThreshold())
                .set(CouponTemplate::getDiscountValue, dto.getDiscountValue())
                .set(CouponTemplate::getTotalCount, dto.getTotalCount())
                .set(CouponTemplate::getValidFrom, dto.getValidFrom())
                .set(CouponTemplate::getValidTo, dto.getValidTo())
                .set(CouponTemplate::getDescription, dto.getDescription())
        );
    }

    @Override
    @Transactional
    public void delete(Long id) {
        CouponTemplate exist = requireById(id);
        if (STATUS_RUNNING.equals(exist.getStatus())) {
            throw new BusinessException(ResultCode.BIZ_ERROR, "进行中的优惠券不可删除,请先结束");
        }
        couponTemplateMapper.deleteById(id);
        try {
            RSemaphore semaphore = redissonClient.getSemaphore(STOCK_KEY_PREFIX + id);
            semaphore.delete();
        } catch (Exception e) {
            log.warn("delete coupon stock semaphore failed, templateId={}", id, e);
        }
    }

    @Override
    @Transactional
    public void issue(Long id) {
        CouponTemplate t = requireById(id);
        if (!STATUS_READY.equals(t.getStatus())) {
            throw new BusinessException(ResultCode.BIZ_ERROR, "只有 READY 状态的券可以发布");
        }
        if (LocalDateTime.now().isAfter(t.getValidTo())) {
            throw new BusinessException(ResultCode.BIZ_ERROR, "优惠券已过期不可发布");
        }
        RSemaphore semaphore = redissonClient.getSemaphore(STOCK_KEY_PREFIX + id);
        semaphore.trySetPermits(t.getTotalCount());
        couponTemplateMapper.update(null,
            new LambdaUpdateWrapper<CouponTemplate>()
                .eq(CouponTemplate::getId, id)
                .eq(CouponTemplate::getStatus, STATUS_READY)
                .set(CouponTemplate::getStatus, STATUS_RUNNING)
        );
    }

    @Override
    @Transactional
    public void end(Long id) {
        CouponTemplate t = requireById(id);
        if (STATUS_ENDED.equals(t.getStatus())) {
            return;
        }
        couponTemplateMapper.update(null,
            new LambdaUpdateWrapper<CouponTemplate>()
                .eq(CouponTemplate::getId, id)
                .set(CouponTemplate::getStatus, STATUS_ENDED)
        );
        try {
            RSemaphore semaphore = redissonClient.getSemaphore(STOCK_KEY_PREFIX + id);
            semaphore.delete();
        } catch (Exception e) {
            log.warn("delete coupon stock semaphore on end failed, templateId={}", id, e);
        }
    }

    @Override
    public CouponTemplateVO detail(Long id) {
        CouponTemplate t = requireById(id);
        return toVO(t);
    }

    @Override
    public PageResult<CouponTemplateVO> listAdmin(String status, Integer page, Integer size) {
        LambdaQueryWrapper<CouponTemplate> w = new LambdaQueryWrapper<CouponTemplate>()
            .eq(CouponTemplate::getDeleted, 0)
            .orderByDesc(CouponTemplate::getCreateTime);
        if (StringUtils.hasText(status)) {
            w.eq(CouponTemplate::getStatus, status);
        }
        Page<CouponTemplate> p = couponTemplateMapper.selectPage(new Page<>(page, size), w);
        List<CouponTemplateVO> list = p.getRecords().stream().map(this::toVO).collect(Collectors.toList());
        return PageResult.of(list, p.getTotal(), p.getCurrent(), p.getSize());
    }

    @Override
    public List<AvailableCouponVO> listAvailable(Long userId) {
        LocalDateTime now = LocalDateTime.now();
        List<CouponTemplate> templates = couponTemplateMapper.selectList(
            new LambdaQueryWrapper<CouponTemplate>()
                .eq(CouponTemplate::getStatus, STATUS_RUNNING)
                .le(CouponTemplate::getValidFrom, now)
                .ge(CouponTemplate::getValidTo, now)
                .eq(CouponTemplate::getDeleted, 0)
                .orderByDesc(CouponTemplate::getCreateTime)
        );

        Set<Long> claimedTemplateIds = new HashSet<>();
        if (userId != null && !templates.isEmpty()) {
            List<UserCoupon> mine = userCouponMapper.selectList(
                new LambdaQueryWrapper<UserCoupon>()
                    .eq(UserCoupon::getUserId, userId)
                    .in(UserCoupon::getTemplateId, templates.stream().map(CouponTemplate::getId).collect(Collectors.toList()))
                    .eq(UserCoupon::getDeleted, 0)
            );
            claimedTemplateIds = mine.stream().map(UserCoupon::getTemplateId).collect(Collectors.toSet());
        }

        Set<Long> finalClaimed = claimedTemplateIds;
        return templates.stream().map(t -> {
            AvailableCouponVO vo = new AvailableCouponVO();
            vo.setTemplateId(t.getId());
            vo.setName(t.getName());
            vo.setType(t.getType());
            vo.setThreshold(t.getThreshold());
            vo.setDiscountValue(t.getDiscountValue());
            int remaining = calcRemaining(t);
            vo.setRemainingCount(remaining);
            vo.setValidFrom(t.getValidFrom());
            vo.setValidTo(t.getValidTo());
            vo.setDescription(t.getDescription());
            vo.setClaimed(finalClaimed.contains(t.getId()));
            return vo;
        }).collect(Collectors.toList());
    }

    private int calcRemaining(CouponTemplate t) {
        int dbRemaining = Math.max(0, t.getTotalCount() - (t.getClaimedCount() == null ? 0 : t.getClaimedCount()));
        try {
            RSemaphore semaphore = redissonClient.getSemaphore(STOCK_KEY_PREFIX + t.getId());
            int redisRemaining = (int) semaphore.availablePermits();
            // 如果 Redis 返回 0 但 DB 显示还有库存，说明 Redis 可能被重置，重新初始化
            if (redisRemaining == 0 && dbRemaining > 0) {
                semaphore.trySetPermits(dbRemaining);
                return dbRemaining;
            }
            return redisRemaining;
        } catch (Exception e) {
            return dbRemaining;
        }
    }

    @Override
    public CouponTemplate requireById(Long id) {
        CouponTemplate t = couponTemplateMapper.selectById(id);
        if (t == null || (t.getDeleted() != null && t.getDeleted() == 1)) {
            throw new BusinessException(ResultCode.COUPON_TEMPLATE_NOT_FOUND);
        }
        return t;
    }

    private CouponTemplateVO toVO(CouponTemplate t) {
        CouponTemplateVO vo = new CouponTemplateVO();
        vo.setId(t.getId());
        vo.setName(t.getName());
        vo.setType(t.getType());
        vo.setThreshold(t.getThreshold());
        vo.setDiscountValue(t.getDiscountValue());
        vo.setTotalCount(t.getTotalCount());
        vo.setClaimedCount(t.getClaimedCount());
        vo.setValidFrom(t.getValidFrom());
        vo.setValidTo(t.getValidTo());
        vo.setStatus(t.getStatus());
        vo.setDescription(t.getDescription());
        vo.setCreateTime(t.getCreateTime());
        return vo;
    }

    private void validateType(String type) {
        if (!TYPE_FULL_REDUCE.equals(type) && !TYPE_DISCOUNT.equals(type) && !TYPE_AMOUNT.equals(type)) {
            throw new BusinessException(ResultCode.PARAM_INVALID, "type 必须是 FULL_REDUCE/DISCOUNT/AMOUNT");
        }
    }
}
