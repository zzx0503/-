package com.bookstore.aop;

import com.bookstore.anno.RateLimit;
import com.bookstore.exception.BusinessException;
import org.aspectj.lang.ProceedingJoinPoint;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.redisson.api.RRateLimiter;
import org.redisson.api.RedissonClient;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RateLimitAspectTest {

    private RedissonClient redisson;
    private RRateLimiter limiter;
    private RateLimitAspect aspect;

    @BeforeEach
    void setup() {
        redisson = mock(RedissonClient.class);
        limiter = mock(RRateLimiter.class);
        when(redisson.getRateLimiter(Mockito.anyString())).thenReturn(limiter);
        aspect = new RateLimitAspect(redisson);

        MockHttpServletRequest req = new MockHttpServletRequest();
        req.setRemoteAddr("127.0.0.1");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(req));
    }

    @Test
    void allows_when_token_available() throws Throwable {
        when(limiter.tryAcquire(1)).thenReturn(true);
        ProceedingJoinPoint pjp = mock(ProceedingJoinPoint.class);
        when(pjp.proceed()).thenReturn("ok");

        Object out = aspect.around(pjp, ann("login", 5, 10));
        assertThat(out).isEqualTo("ok");
    }

    @Test
    void rejects_when_no_token() throws Throwable {
        when(limiter.tryAcquire(1)).thenReturn(false);
        ProceedingJoinPoint pjp = mock(ProceedingJoinPoint.class);

        assertThatThrownBy(() -> aspect.around(pjp, ann("login", 5, 10)))
            .isInstanceOf(BusinessException.class);
    }

    private RateLimit ann(String key, int qps, int burst) {
        return new RateLimit() {
            @Override public Class<? extends java.lang.annotation.Annotation> annotationType() { return RateLimit.class; }
            @Override public String key() { return key; }
            @Override public int qps() { return qps; }
            @Override public int burst() { return burst; }
        };
    }
}
