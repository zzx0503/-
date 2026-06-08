package com.bookstore.utils;

import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtUtilTest {

    private JwtUtil jwt;

    @BeforeEach
    void setup() {
        SecretKey key = Keys.hmacShaKeyFor("THIS_IS_A_PLACEHOLDER_SECRET_KEY_FOR_TESTING_ONLY_1234567890ABCD".getBytes());
        jwt = new JwtUtil(key, Duration.ofHours(2), Duration.ofDays(7));
    }

    @Test
    void issue_then_parse_recovers_claims() {
        String token = jwt.issueAccessToken(42L, "alice", "USER");
        JwtUtil.UserClaims c = jwt.parseAccess(token);
        assertThat(c.userId()).isEqualTo(42L);
        assertThat(c.username()).isEqualTo("alice");
        assertThat(c.role()).isEqualTo("USER");
    }

    @Test
    void access_and_refresh_have_different_type_claim() {
        String access = jwt.issueAccessToken(1L, "u", "USER");
        String refresh = jwt.issueRefreshToken(1L, "u", "USER");
        assertThat(jwt.parseAccess(access)).isNotNull();
        assertThatThrownBy(() -> jwt.parseAccess(refresh))
            .isInstanceOf(JwtException.class);
    }

    @Test
    void tampered_signature_is_rejected() {
        String token = jwt.issueAccessToken(1L, "u", "USER");
        String tampered = token.substring(0, token.length() - 2) + "ab";
        assertThatThrownBy(() -> jwt.parseAccess(tampered))
            .isInstanceOf(JwtException.class);
    }
}
