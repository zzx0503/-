package com.bookstore.service.impl;

import com.bookstore.service.TokenBlacklistService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;

@Service
@RequiredArgsConstructor
public class TokenBlacklistServiceImpl implements TokenBlacklistService {

    private static final String PREFIX = "blacklist:";

    private final StringRedisTemplate redis;

    @Override
    public void revoke(String jti, long expiresAtEpochMs) {
        long remaining = expiresAtEpochMs - Instant.now().toEpochMilli();
        if (remaining <= 0) return;
        redis.opsForValue().set(PREFIX + jti, "1", Duration.ofMillis(remaining));
    }

    @Override
    public boolean isRevoked(String jti) {
        Boolean has = redis.hasKey(PREFIX + jti);
        return Boolean.TRUE.equals(has);
    }
}
