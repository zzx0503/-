package com.bookstore.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.bookstore.domain.po.CouponTemplate;
import com.bookstore.domain.po.UserCoupon;
import com.bookstore.domain.vo.coupon.ClaimResultVO;
import com.bookstore.domain.vo.coupon.UserCouponVO;
import com.bookstore.exception.BusinessException;
import com.bookstore.mapper.CouponTemplateMapper;
import com.bookstore.mapper.UserCouponMapper;
import com.bookstore.response.ResultCode;
import com.bookstore.service.CouponTemplateService;
import com.bookstore.service.UserCouponService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RSemaphore;
import org.redisson.api.RedissonClient;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserCouponServiceImpl implements UserCouponService {

    public static final String STATUS_UNUSED = "UNUSED";
    public static final String STATUS_LOCKED = "LOCKED";
    public static final String STATUS_USED = "USED";
    public static final String STATUS_EXPIRED = "EXPIRED";

    private static final String LOCK_KEY_PREFIX = "user_coupon:";
    private static final char[] CODE_CHARS = "ABCDEFGHJKMNPQRSTUVWXYZ23456789".toCharArray();
    private static final int CODE_LENGTH = 16;
    private static final long LOCK_WAIT_SECONDS = 3;
    private static final long LOCK_LEASE_SECONDS = 10;
    private static final long STUCK_LOCKED_RELEASE_HOURS = 24;

    private final UserCouponMapper userCouponMapper;
    private final CouponTemplateMapper couponTemplateMapper;
    private final CouponTemplateService couponTemplateService;
    private final RedissonClient redissonClient;

    private final SecureRandom random = new SecureRandom();

    @Override
    @Transactional
    public ClaimResultVO claim(Long userId, Long templateId) {
        CouponTemplate t = couponTemplateService.requireById(templateId);
        if (!CouponTemplateServiceImpl.STATUS_RUNNING.equals(t.getStatus())) {
            throw new BusinessException(ResultCode.COUPON_NOT_AVAILABLE);
        }
        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(t.getValidFrom()) || now.isAfter(t.getValidTo())) {
            throw new BusinessException(ResultCode.COUPON_NOT_AVAILABLE, "优惠券不在领取时间范围内");
        }

        Long existing = userCouponMapper.selectCount(
            new LambdaQueryWrapper<UserCoupon>()
                .eq(UserCoupon::getUserId, userId)
                .eq(UserCoupon::getTemplateId, templateId)
                .eq(UserCoupon::getDeleted, 0)
        );
        if (existing != null && existing > 0) {
            throw new BusinessException(ResultCode.COUPON_NOT_AVAILABLE, "已领取过该优惠券");
        }

        RSemaphore semaphore = redissonClient.getSemaphore(CouponTemplateServiceImpl.STOCK_KEY_PREFIX + templateId);
        boolean acquired;
        try {
            acquired = semaphore.tryAcquire(1, 200, TimeUnit.MILLISECONDS);
            // 如果没抢到且 DB 还有库存，可能是 Redis 被重置，尝试重新初始化
            if (!acquired) {
                int dbRemaining = Math.max(0, t.getTotalCount() - (t.getClaimedCount() == null ? 0 : t.getClaimedCount()));
                if (dbRemaining > 0) {
                    semaphore.trySetPermits(dbRemaining);
                    acquired = semaphore.tryAcquire(1, 200, TimeUnit.MILLISECONDS);
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BusinessException(ResultCode.COUPON_OUT_OF_STOCK);
        }
        if (!acquired) {
            throw new BusinessException(ResultCode.COUPON_OUT_OF_STOCK);
        }

        try {
            UserCoupon uc = new UserCoupon();
            uc.setUserId(userId);
            uc.setTemplateId(templateId);
            uc.setCode(generateUniqueCode());
            uc.setStatus(STATUS_UNUSED);
            uc.setExpireTime(t.getValidTo());
            userCouponMapper.insert(uc);

            couponTemplateMapper.update(null,
                new LambdaUpdateWrapper<CouponTemplate>()
                    .eq(CouponTemplate::getId, templateId)
                    .setSql("claimed_count = claimed_count + 1")
            );

            ClaimResultVO vo = new ClaimResultVO();
            vo.setUserCouponId(uc.getId());
            vo.setCode(uc.getCode());
            vo.setStatus(uc.getStatus());
            return vo;
        } catch (RuntimeException e) {
            try {
                semaphore.release();
            } catch (Exception ignore) {
            }
            throw e;
        }
    }

    @Override
    public List<UserCouponVO> listMine(Long userId, String status) {
        LambdaQueryWrapper<UserCoupon> w = new LambdaQueryWrapper<UserCoupon>()
            .eq(UserCoupon::getUserId, userId)
            .eq(UserCoupon::getDeleted, 0)
            .orderByDesc(UserCoupon::getCreateTime);
        if (StringUtils.hasText(status)) {
            w.eq(UserCoupon::getStatus, status);
        }
        List<UserCoupon> coupons = userCouponMapper.selectList(w);
        if (coupons.isEmpty()) {
            return List.of();
        }
        Map<Long, CouponTemplate> templateMap = loadTemplateMap(
            coupons.stream().map(UserCoupon::getTemplateId).distinct().collect(Collectors.toList())
        );
        LocalDateTime now = LocalDateTime.now();
        return coupons.stream().map(uc -> toVO(uc, templateMap.get(uc.getTemplateId()), now)).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public UserCoupon lockForOrder(Long userId, Long userCouponId, String orderNo) {
        if (userCouponId == null) {
            return null;
        }
        RLock lock = redissonClient.getLock(LOCK_KEY_PREFIX + userCouponId);
        boolean locked;
        try {
            locked = lock.tryLock(LOCK_WAIT_SECONDS, LOCK_LEASE_SECONDS, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BusinessException(ResultCode.COUPON_LOCKED_BY_OTHER);
        }
        if (!locked) {
            throw new BusinessException(ResultCode.COUPON_LOCKED_BY_OTHER);
        }
        try {
            UserCoupon uc = userCouponMapper.selectById(userCouponId);
            if (uc == null || (uc.getDeleted() != null && uc.getDeleted() == 1)) {
                throw new BusinessException(ResultCode.COUPON_NOT_FOUND);
            }
            if (!uc.getUserId().equals(userId)) {
                throw new BusinessException(ResultCode.FORBIDDEN);
            }
            if (!STATUS_UNUSED.equals(uc.getStatus())) {
                throw new BusinessException(ResultCode.COUPON_NOT_USABLE, "该优惠券当前不可用");
            }
            if (uc.getExpireTime() != null && LocalDateTime.now().isAfter(uc.getExpireTime())) {
                throw new BusinessException(ResultCode.COUPON_NOT_USABLE, "优惠券已过期");
            }
            int affected = userCouponMapper.update(null,
                new LambdaUpdateWrapper<UserCoupon>()
                    .eq(UserCoupon::getId, userCouponId)
                    .eq(UserCoupon::getStatus, STATUS_UNUSED)
                    .set(UserCoupon::getStatus, STATUS_LOCKED)
                    .set(UserCoupon::getLockedOrderNo, orderNo)
            );
            if (affected == 0) {
                throw new BusinessException(ResultCode.COUPON_NOT_USABLE);
            }
            uc.setStatus(STATUS_LOCKED);
            uc.setLockedOrderNo(orderNo);
            return uc;
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    @Override
    @Transactional
    public void useForOrder(Long userId, Long userCouponId, String orderNo) {
        if (userCouponId == null) {
            return;
        }
        RLock lock = redissonClient.getLock(LOCK_KEY_PREFIX + userCouponId);
        boolean locked;
        try {
            locked = lock.tryLock(LOCK_WAIT_SECONDS, LOCK_LEASE_SECONDS, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BusinessException(ResultCode.COUPON_LOCKED_BY_OTHER);
        }
        if (!locked) {
            throw new BusinessException(ResultCode.COUPON_LOCKED_BY_OTHER);
        }
        try {
            UserCoupon uc = userCouponMapper.selectById(userCouponId);
            if (uc == null || (uc.getDeleted() != null && uc.getDeleted() == 1)) {
                throw new BusinessException(ResultCode.COUPON_NOT_FOUND);
            }
            if (!uc.getUserId().equals(userId)) {
                throw new BusinessException(ResultCode.FORBIDDEN);
            }
            if (STATUS_USED.equals(uc.getStatus()) && orderNo.equals(uc.getUsedOrderNo())) {
                return;
            }
            if (!STATUS_LOCKED.equals(uc.getStatus()) || !orderNo.equals(uc.getLockedOrderNo())) {
                throw new BusinessException(ResultCode.COUPON_NOT_USABLE);
            }
            int affected = userCouponMapper.update(null,
                new LambdaUpdateWrapper<UserCoupon>()
                    .eq(UserCoupon::getId, userCouponId)
                    .eq(UserCoupon::getStatus, STATUS_LOCKED)
                    .eq(UserCoupon::getLockedOrderNo, orderNo)
                    .set(UserCoupon::getStatus, STATUS_USED)
                    .set(UserCoupon::getUsedOrderNo, orderNo)
                    .set(UserCoupon::getUsedAt, LocalDateTime.now())
                    .set(UserCoupon::getLockedOrderNo, null)
            );
            if (affected == 0) {
                throw new BusinessException(ResultCode.COUPON_NOT_USABLE);
            }
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    @Override
    @Transactional
    public void releaseForOrder(Long userId, Long userCouponId, String orderNo) {
        if (userCouponId == null) {
            return;
        }
        RLock lock = redissonClient.getLock(LOCK_KEY_PREFIX + userCouponId);
        boolean locked;
        try {
            locked = lock.tryLock(LOCK_WAIT_SECONDS, LOCK_LEASE_SECONDS, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return;
        }
        if (!locked) {
            return;
        }
        try {
            UserCoupon uc = userCouponMapper.selectById(userCouponId);
            if (uc == null) {
                return;
            }
            if (userId != null && !uc.getUserId().equals(userId)) {
                return;
            }
            if (!STATUS_LOCKED.equals(uc.getStatus()) || !orderNo.equals(uc.getLockedOrderNo())) {
                return;
            }
            userCouponMapper.update(null,
                new LambdaUpdateWrapper<UserCoupon>()
                    .eq(UserCoupon::getId, userCouponId)
                    .eq(UserCoupon::getStatus, STATUS_LOCKED)
                    .eq(UserCoupon::getLockedOrderNo, orderNo)
                    .set(UserCoupon::getStatus, STATUS_UNUSED)
                    .set(UserCoupon::getLockedOrderNo, null)
            );
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    @Override
    @Scheduled(cron = "0 */15 * * * ?")
    public int autoReleaseStuckLocked() {
        LocalDateTime cutoff = LocalDateTime.now().minusHours(STUCK_LOCKED_RELEASE_HOURS);
        List<UserCoupon> stuck = userCouponMapper.selectList(
            new LambdaQueryWrapper<UserCoupon>()
                .eq(UserCoupon::getStatus, STATUS_LOCKED)
                .lt(UserCoupon::getUpdateTime, cutoff)
                .eq(UserCoupon::getDeleted, 0)
        );
        int count = 0;
        for (UserCoupon uc : stuck) {
            try {
                releaseForOrder(uc.getUserId(), uc.getId(), uc.getLockedOrderNo());
                count++;
            } catch (Exception e) {
                log.warn("auto release stuck locked coupon failed, id={}", uc.getId(), e);
            }
        }
        if (count > 0) {
            log.info("auto released {} stuck locked coupons", count);
        }
        return count;
    }

    @Override
    @Scheduled(cron = "0 */30 * * * ?")
    public int autoExpire() {
        LocalDateTime now = LocalDateTime.now();
        int affected = userCouponMapper.update(null,
            new LambdaUpdateWrapper<UserCoupon>()
                .in(UserCoupon::getStatus, List.of(STATUS_UNUSED, STATUS_LOCKED))
                .lt(UserCoupon::getExpireTime, now)
                .set(UserCoupon::getStatus, STATUS_EXPIRED)
        );
        if (affected > 0) {
            log.info("auto expired {} user coupons", affected);
        }
        return affected;
    }

    private String generateUniqueCode() {
        for (int attempt = 0; attempt < 5; attempt++) {
            String code = randomCode();
            Long existing = userCouponMapper.selectCount(
                new LambdaQueryWrapper<UserCoupon>().eq(UserCoupon::getCode, code)
            );
            if (existing == null || existing == 0) {
                return code;
            }
        }
        throw new BusinessException(ResultCode.SERVER_ERROR, "生成优惠券码失败,请重试");
    }

    private String randomCode() {
        StringBuilder sb = new StringBuilder(CODE_LENGTH);
        for (int i = 0; i < CODE_LENGTH; i++) {
            sb.append(CODE_CHARS[random.nextInt(CODE_CHARS.length)]);
        }
        return sb.toString();
    }

    private Map<Long, CouponTemplate> loadTemplateMap(List<Long> templateIds) {
        if (templateIds == null || templateIds.isEmpty()) {
            return new HashMap<>();
        }
        List<CouponTemplate> templates = couponTemplateMapper.selectList(
            new LambdaQueryWrapper<CouponTemplate>().in(CouponTemplate::getId, templateIds)
        );
        return templates.stream().collect(Collectors.toMap(CouponTemplate::getId, x -> x));
    }

    private UserCouponVO toVO(UserCoupon uc, CouponTemplate t, LocalDateTime now) {
        UserCouponVO vo = new UserCouponVO();
        vo.setId(uc.getId());
        vo.setTemplateId(uc.getTemplateId());
        vo.setCode(uc.getCode());
        vo.setStatus(resolveDisplayStatus(uc, now));
        vo.setLockedOrderNo(uc.getLockedOrderNo());
        vo.setUsedOrderNo(uc.getUsedOrderNo());
        vo.setUsedAt(uc.getUsedAt());
        vo.setExpireTime(uc.getExpireTime());
        vo.setCreateTime(uc.getCreateTime());
        if (t != null) {
            vo.setName(t.getName());
            vo.setType(t.getType());
            vo.setThreshold(t.getThreshold());
            vo.setDiscountValue(t.getDiscountValue());
            vo.setDescription(t.getDescription());
        }
        return vo;
    }

    private String resolveDisplayStatus(UserCoupon uc, LocalDateTime now) {
        if (STATUS_USED.equals(uc.getStatus()) || STATUS_EXPIRED.equals(uc.getStatus())) {
            return uc.getStatus();
        }
        if (uc.getExpireTime() != null && now.isAfter(uc.getExpireTime())) {
            return STATUS_EXPIRED;
        }
        return uc.getStatus();
    }
}
