package com.bookstore.service.impl;
import com.bookstore.service.TokenBlacklistService;

import com.bookstore.exception.BusinessException;
import com.bookstore.response.ResultCode;
import com.bookstore.utils.JwtUtil;
import com.bookstore.domain.dto.auth.RegisterDTO;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceRegisterTest {

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

    private RegisterDTO sampleDto() {
        RegisterDTO dto = new RegisterDTO();
        dto.setUsername("alice");
        dto.setPassword("YOUR_PASSWORD");
        dto.setPhone("13800000001");
        return dto;
    }

    @Test
    void register_succeeds_and_returns_tokens() {
        when(userMapper.selectCount(any())).thenReturn(0L);
        when(userMapper.insert(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(42L);
            return 1;
        });

        TokenVO vo = authService.register(sampleDto());

        assertThat(vo.getAccessToken()).isNotBlank();
        assertThat(vo.getRefreshToken()).isNotBlank();
        assertThat(vo.getUser().getId()).isEqualTo(42L);
        assertThat(vo.getUser().getUsername()).isEqualTo("alice");
        assertThat(vo.getUser().getRole()).isEqualTo("USER");
    }

    @Test
    void register_fails_when_username_taken() {
        when(userMapper.selectCount(any())).thenReturn(1L);

        assertThatThrownBy(() -> authService.register(sampleDto()))
            .isInstanceOf(BusinessException.class)
            .extracting("resultCode").isEqualTo(ResultCode.USERNAME_TAKEN);
    }
}
