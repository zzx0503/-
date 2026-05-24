package com.bookstore.service.impl;
import com.bookstore.service.TokenBlacklistService;

import com.bookstore.exception.BusinessException;
import com.bookstore.response.ResultCode;
import com.bookstore.utils.JwtUtil;
import com.bookstore.utils.PasswordUtil;
import com.bookstore.domain.dto.auth.LoginDTO;
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
class AuthServiceLoginTest {

    @Mock UserMapper userMapper;
    @Mock TokenBlacklistService blacklist;

    AuthServiceImpl authService;

    @BeforeEach
    void setUp() {
        String secret = "test-secret-must-be-long-enough-for-hs256-test-secret-must-be-long";
        JwtUtil jwtUtil = new JwtUtil(
            Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8)),
            Duration.ofHours(2),
            Duration.ofDays(7)
        );
        authService = new AuthServiceImpl(userMapper, jwtUtil, blacklist);
    }

    private User existingUser() {
        User u = new User();
        u.setId(1L);
        u.setUsername("alice");
        u.setPhone("13800000001");
        u.setPasswordHash(PasswordUtil.encode("hello123"));
        u.setRole("USER");
        u.setStatus(1);
        u.setNickname("Alice");
        return u;
    }

    @Test
    void login_by_username_succeeds() {
        when(userMapper.selectOne(any())).thenReturn(existingUser());

        LoginDTO dto = new LoginDTO();
        dto.setAccount("alice");
        dto.setPassword("hello123");

        TokenVO vo = authService.login(dto);
        assertThat(vo.getAccessToken()).isNotBlank();
        assertThat(vo.getUser().getUsername()).isEqualTo("alice");
    }

    @Test
    void login_account_not_found() {
        when(userMapper.selectOne(any())).thenReturn(null);

        LoginDTO dto = new LoginDTO();
        dto.setAccount("ghost");
        dto.setPassword("xxxxxx");

        assertThatThrownBy(() -> authService.login(dto))
            .isInstanceOf(BusinessException.class)
            .extracting("resultCode").isEqualTo(ResultCode.ACCOUNT_NOT_FOUND);
    }

    @Test
    void login_password_wrong() {
        when(userMapper.selectOne(any())).thenReturn(existingUser());

        LoginDTO dto = new LoginDTO();
        dto.setAccount("alice");
        dto.setPassword("WRONG-PASSWORD");

        assertThatThrownBy(() -> authService.login(dto))
            .isInstanceOf(BusinessException.class)
            .extracting("resultCode").isEqualTo(ResultCode.PASSWORD_WRONG);
    }

    @Test
    void login_account_disabled() {
        User u = existingUser();
        u.setStatus(0);
        when(userMapper.selectOne(any())).thenReturn(u);

        LoginDTO dto = new LoginDTO();
        dto.setAccount("alice");
        dto.setPassword("hello123");

        assertThatThrownBy(() -> authService.login(dto))
            .isInstanceOf(BusinessException.class)
            .extracting("resultCode").isEqualTo(ResultCode.ACCOUNT_DISABLED);
    }
}
