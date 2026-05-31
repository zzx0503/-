package com.bookstore.app.config;

import com.bookstore.utils.JwtUtil;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

@Configuration
public class JwtConfig {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.access-token-ttl}")
    private Duration accessTtl;

    @Value("${jwt.refresh-token-ttl}")
    private Duration refreshTtl;

    @Bean
    public JwtUtil jwtUtil() {
        SecretKey key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        return new JwtUtil(key, accessTtl, refreshTtl);
    }
}
