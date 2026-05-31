package com.bookstore.utils;

import com.bookstore.exception.AuthException;
import com.bookstore.response.ResultCode;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;

import javax.crypto.SecretKey;
import java.time.Duration;
import java.util.Date;
import java.util.UUID;

public class JwtUtil {

    public static final String TYPE_ACCESS  = "access";
    public static final String TYPE_REFRESH = "refresh";

    private final SecretKey key;
    private final Duration accessTtl;
    private final Duration refreshTtl;

    public JwtUtil(SecretKey key, Duration accessTtl, Duration refreshTtl) {
        this.key = key;
        this.accessTtl = accessTtl;
        this.refreshTtl = refreshTtl;
    }

    public String issueAccessToken(Long userId, String username, String role) {
        return issue(userId, username, role, TYPE_ACCESS, accessTtl);
    }

    public String issueRefreshToken(Long userId, String username, String role) {
        return issue(userId, username, role, TYPE_REFRESH, refreshTtl);
    }

    public long getAccessTtlSeconds() {
        return accessTtl.getSeconds();
    }

    private String issue(Long userId, String username, String role, String type, Duration ttl) {
        long now = System.currentTimeMillis();
        return Jwts.builder()
            .id(UUID.randomUUID().toString())
            .subject(String.valueOf(userId))
            .claim("username", username)
            .claim("role", role)
            .claim("type", type)
            .issuedAt(new Date(now))
            .expiration(new Date(now + ttl.toMillis()))
            .signWith(key)
            .compact();
    }

    public UserClaims parseAccess(String token) {
        return parse(token, TYPE_ACCESS);
    }

    public UserClaims parseRefresh(String token) {
        return parse(token, TYPE_REFRESH);
    }

    public UserClaims parse(String token) {
        try {
            Claims claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
            return new UserClaims(
                Long.parseLong(claims.getSubject()),
                claims.get("username", String.class),
                claims.get("role", String.class),
                claims.getId(),
                claims.getExpiration().toInstant().toEpochMilli()
            );
        } catch (JwtException | IllegalArgumentException e) {
            throw new AuthException(ResultCode.TOKEN_INVALID);
        }
    }

    public String getTokenType(String token) {
        try {
            return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload()
                .get("type", String.class);
        } catch (JwtException | IllegalArgumentException e) {
            throw new AuthException(ResultCode.TOKEN_INVALID);
        }
    }

    private UserClaims parse(String token, String expectedType) {
        Claims claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
        String type = claims.get("type", String.class);
        if (!expectedType.equals(type)) {
            throw new JwtException("token type mismatch: expected " + expectedType + ", got " + type);
        }
        return new UserClaims(
            Long.parseLong(claims.getSubject()),
            claims.get("username", String.class),
            claims.get("role", String.class),
            claims.getId(),
            claims.getExpiration().toInstant().toEpochMilli()
        );
    }

    public record UserClaims(Long userId, String username, String role, String jti, long expiresAtEpochMs) {}
}
