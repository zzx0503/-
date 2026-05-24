package com.bookstore.service.impl;
import com.bookstore.service.TokenBlacklistService;

import com.bookstore.exception.AuthException;
import com.bookstore.exception.BusinessException;
import com.bookstore.response.ResultCode;
import com.bookstore.utils.JwtUtil;
import com.bookstore.domain.dto.auth.RefreshTokenDTO;
import com.bookstore.domain.po.User;
import com.bookstore.domain.vo.auth.TokenVO;
import com.bookstore.mapper.UserMapper;
import com.bookstore.service.impl.AuthServiceImpl;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.charset.StandardCharsets;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceRefreshTest {

    @Mock UserMapper userMapper;
    @Mock TokenBlacklistService blacklist;

    JwtUtil jwtUtil;
    AuthServiceImpl authService;

    @BeforeEach
    void setUp() {
        String secret = "test-secret-must-be-long-enough-for-hs256-test-secret-must-be-long";
        jwtUtil = new JwtUtil(
            Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8)),
            Duration.ofHours(2),
            Duration.ofDays(7)
        );
        authService = new AuthServiceImpl(userMapper, jwtUtil, blacklist);
    }

    @Test
    void refresh_with_valid_refresh_token_returns_new_pair() {
        User u = new User();
        u.setId(1L);
        u.setUsername("alice");
        u.setRole("USER");
        u.setStatus(1);
        u.setNickname("Alice");
        when(userMapper.selectById(1L)).thenReturn(u);
        when(blacklist.isRevoked(anyString())).thenReturn(false);

        String refresh = jwtUtil.issueRefreshToken(1L, "alice", "USER");
        RefreshTokenDTO dto = new RefreshTokenDTO();
        dto.setRefreshToken(refresh);

        TokenVO vo = authService.refresh(dto);
        assertThat(vo.getAccessToken()).isNotBlank();
        assertThat(vo.getRefreshToken()).isNotBlank().isNotEqualTo(refresh);
    }

    @Test
    void refresh_rejects_access_token() {
        String access = jwtUtil.issueAccessToken(1L, "alice", "USER");
        RefreshTokenDTO dto = new RefreshTokenDTO();
        dto.setRefreshToken(access);

        assertThatThrownBy(() -> authService.refresh(dto))
            .isInstanceOf(BusinessException.class)
            .extracting("resultCode").isEqualTo(ResultCode.TOKEN_TYPE_MISMATCH);
    }

    @Test
    void refresh_rejects_invalid_token() {
        RefreshTokenDTO dto = new RefreshTokenDTO();
        dto.setRefreshToken("not-a-real-jwt");

        assertThatThrownBy(() -> authService.refresh(dto))
            .isInstanceOf(AuthException.class)
            .extracting("resultCode").isEqualTo(ResultCode.TOKEN_INVALID);
    }
}
