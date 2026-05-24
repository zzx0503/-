package com.bookstore.service.impl;
import com.bookstore.service.TokenBlacklistService;

import com.bookstore.service.impl.TokenBlacklistServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TokenBlacklistServiceTest {

    private StringRedisTemplate redis;
    @SuppressWarnings("unchecked")
    private ValueOperations<String, String> ops;
    private TokenBlacklistService svc;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setup() {
        redis = mock(StringRedisTemplate.class);
        ops = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(ops);
        svc = new TokenBlacklistServiceImpl(redis);
    }

    @Test
    void revoke_writes_jti_with_remaining_ttl() {
        long expiresAt = System.currentTimeMillis() + 60_000;
        svc.revoke("jti-1", expiresAt);
        verify(ops).set(eq("blacklist:jti-1"), eq("1"), any());
    }

    @Test
    void isRevoked_returns_true_when_present() {
        when(redis.hasKey("blacklist:jti-2")).thenReturn(true);
        assertThat(svc.isRevoked("jti-2")).isTrue();
    }

    private static String eq(String s) { return org.mockito.ArgumentMatchers.eq(s); }
}
