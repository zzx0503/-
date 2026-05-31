package com.bookstore.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bookstore.domain.po.CheckinRewardRule;
import com.bookstore.domain.po.User;
import com.bookstore.domain.po.UserCheckin;
import com.bookstore.domain.po.WalletTransaction;
import com.bookstore.domain.vo.checkin.CheckinRecordVO;
import com.bookstore.domain.vo.checkin.CheckinResultVO;
import com.bookstore.domain.vo.checkin.CheckinStatusVO;
import com.bookstore.exception.BusinessException;
import com.bookstore.mapper.CheckinRewardRuleMapper;
import com.bookstore.mapper.UserCheckinMapper;
import com.bookstore.mapper.UserMapper;
import com.bookstore.mapper.WalletTransactionMapper;
import com.bookstore.response.PageResult;
import com.bookstore.response.ResultCode;
import com.bookstore.service.CheckinService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class CheckinServiceImpl implements CheckinService {

    private static final String CHECKIN_LOCK_PREFIX = "checkin:lock:";
    private static final String CHECKIN_STATUS_PREFIX = "checkin:today:";
    private static final BigDecimal BASE_REWARD = new BigDecimal("0.10");

    private final UserCheckinMapper userCheckinMapper;
    private final CheckinRewardRuleMapper rewardRuleMapper;
    private final UserMapper userMapper;
    private final WalletTransactionMapper walletTransactionMapper;
    private final RedissonClient redissonClient;
    private final StringRedisTemplate stringRedisTemplate;

    @Override
    @Transactional
    public CheckinResultVO checkin(Long userId) {
        LocalDate today = LocalDate.now();
        String lockKey = CHECKIN_LOCK_PREFIX + userId + ":" + today;
        RLock lock = redissonClient.getLock(lockKey);

        boolean locked = false;
        try {
            locked = lock.tryLock(3, 5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BusinessException(ResultCode.SERVER_ERROR, "操作被中断");
        }
        if (!locked) {
            throw new BusinessException(ResultCode.BIZ_ERROR, "操作过于频繁，请稍后再试");
        }

        try {
            String cacheKey = CHECKIN_STATUS_PREFIX + userId;
            String cached = stringRedisTemplate.opsForValue().get(cacheKey);
            if ("1".equals(cached)) {
                throw new BusinessException(ResultCode.BIZ_ERROR, "今日已签到");
            }

            UserCheckin existing = userCheckinMapper.selectByUserAndDate(userId, today);
            if (existing != null) {
                stringRedisTemplate.opsForValue().set(cacheKey, "1", Duration.ofHours(26));
                throw new BusinessException(ResultCode.BIZ_ERROR, "今日已签到");
            }

            UserCheckin lastCheckin = userCheckinMapper.selectLatestByUser(userId);
            int consecutive = 1;
            if (lastCheckin != null && lastCheckin.getCheckinDate().equals(today.minusDays(1))) {
                consecutive = lastCheckin.getConsecutiveDays() + 1;
            }

            BigDecimal reward = BASE_REWARD;
            CheckinRewardRule bonusRule = rewardRuleMapper.selectOne(
                new LambdaQueryWrapper<CheckinRewardRule>()
                    .eq(CheckinRewardRule::getConsecutiveDays, consecutive)
                    .eq(CheckinRewardRule::getDeleted, 0)
            );
            if (bonusRule != null) {
                reward = reward.add(bonusRule.getRewardAmount());
            }

            UserCheckin record = new UserCheckin();
            record.setUserId(userId);
            record.setCheckinDate(today);
            record.setConsecutiveDays(consecutive);
            record.setRewardAmount(reward);
            userCheckinMapper.insert(record);

            User userBefore = userMapper.selectById(userId);
            BigDecimal balanceBefore = userBefore.getWalletBalance() == null ? BigDecimal.ZERO : userBefore.getWalletBalance();

            userMapper.update(null,
                new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<User>()
                    .eq(User::getId, userId)
                    .setSql("wallet_balance = COALESCE(wallet_balance, 0) + " + reward.toPlainString())
            );

            User userAfter = userMapper.selectById(userId);
            BigDecimal balanceAfter = userAfter.getWalletBalance() == null ? BigDecimal.ZERO : userAfter.getWalletBalance();

            WalletTransaction tx = new WalletTransaction();
            tx.setUserId(userId);
            tx.setType("CHECKIN");
            tx.setAmount(reward);
            tx.setBalanceBefore(balanceBefore);
            tx.setBalanceAfter(balanceAfter);
            tx.setRemark("每日签到奖励" + (consecutive > 1 ? "（连续" + consecutive + "天）" : ""));
            walletTransactionMapper.insert(tx);

            stringRedisTemplate.opsForValue().set(cacheKey, "1", Duration.ofHours(26));

            return new CheckinResultVO(true, consecutive, reward, today);
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    @Override
    public CheckinStatusVO getStatus(Long userId) {
        LocalDate today = LocalDate.now();
        String cacheKey = CHECKIN_STATUS_PREFIX + userId;
        String cached = stringRedisTemplate.opsForValue().get(cacheKey);
        boolean checkedInToday = "1".equals(cached);

        if (!checkedInToday) {
            UserCheckin todayRecord = userCheckinMapper.selectByUserAndDate(userId, today);
            checkedInToday = todayRecord != null;
            if (checkedInToday) {
                stringRedisTemplate.opsForValue().set(cacheKey, "1", Duration.ofHours(26));
            }
        }

        UserCheckin latest = userCheckinMapper.selectLatestByUser(userId);
        int currentStreak = 0;
        if (latest != null) {
            if (latest.getCheckinDate().equals(today)) {
                currentStreak = latest.getConsecutiveDays();
            } else if (latest.getCheckinDate().equals(today.minusDays(1))) {
                currentStreak = latest.getConsecutiveDays();
            }
        }

        LocalDate monthStart = today.withDayOfMonth(1);
        List<LocalDate> checkedDates = userCheckinMapper.selectDatesInRange(userId, monthStart, today);

        int wouldBeConsecutive = checkedInToday ? currentStreak : currentStreak + 1;
        if (!checkedInToday && currentStreak == 0 && latest != null && !latest.getCheckinDate().equals(today.minusDays(1))) {
            wouldBeConsecutive = 1;
        }

        CheckinStatusVO vo = new CheckinStatusVO();
        vo.setCheckedInToday(checkedInToday);
        vo.setCurrentStreak(currentStreak);
        vo.setTodayReward(calculateReward(wouldBeConsecutive));
        vo.setMonthCheckedDates(checkedDates);
        return vo;
    }

    @Override
    public PageResult<CheckinRecordVO> getHistory(Long userId, Integer page, Integer size) {
        LambdaQueryWrapper<UserCheckin> w = new LambdaQueryWrapper<UserCheckin>()
            .eq(UserCheckin::getUserId, userId)
            .eq(UserCheckin::getDeleted, 0)
            .orderByDesc(UserCheckin::getCheckinDate);
        Page<UserCheckin> p = userCheckinMapper.selectPage(new Page<>(page, size), w);
        List<CheckinRecordVO> list = p.getRecords().stream()
            .map(r -> {
                CheckinRecordVO v = new CheckinRecordVO();
                v.setCheckinDate(r.getCheckinDate());
                v.setConsecutiveDays(r.getConsecutiveDays());
                v.setRewardAmount(r.getRewardAmount());
                return v;
            })
            .toList();
        return PageResult.of(list, p.getTotal(), (int) p.getCurrent(), (int) p.getSize());
    }

    private BigDecimal calculateReward(int consecutiveDays) {
        BigDecimal reward = BASE_REWARD;
        CheckinRewardRule bonus = rewardRuleMapper.selectOne(
            new LambdaQueryWrapper<CheckinRewardRule>()
                .eq(CheckinRewardRule::getConsecutiveDays, consecutiveDays)
                .eq(CheckinRewardRule::getDeleted, 0)
        );
        if (bonus != null) {
            reward = reward.add(bonus.getRewardAmount());
        }
        return reward;
    }
}
