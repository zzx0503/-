package com.bookstore.aop;

import com.bookstore.anno.RateLimit;
import com.bookstore.exception.BusinessException;
import com.bookstore.response.ResultCode;
import com.bookstore.utils.IpUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.redisson.api.RRateLimiter;
import org.redisson.api.RateIntervalUnit;
import org.redisson.api.RateType;
import org.redisson.api.RedissonClient;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Slf4j
@Aspect
@Component
@Profile("!test")
@RequiredArgsConstructor
public class RateLimitAspect {

    private final RedissonClient redisson;

    @Around("@annotation(rl)")
    public Object around(ProceedingJoinPoint pjp, RateLimit rl) throws Throwable {
        String key = "rate:" + rl.key() + ":" + currentIp();
        RRateLimiter limiter = redisson.getRateLimiter(key);
        limiter.trySetRate(RateType.OVERALL, rl.burst(), 1, RateIntervalUnit.SECONDS);
        if (!limiter.tryAcquire(1)) {
            log.warn("rate limit hit key={}", key);
            throw new BusinessException(ResultCode.RATE_LIMIT);
        }
        return pjp.proceed();
    }

    private String currentIp() {
        ServletRequestAttributes attrs =
            (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs == null) return "unknown";
        HttpServletRequest req = attrs.getRequest();
        return IpUtil.getClientIp(req);
    }
}
