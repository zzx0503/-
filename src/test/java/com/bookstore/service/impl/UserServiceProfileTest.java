package com.bookstore.service.impl;

import com.bookstore.exception.BusinessException;
import com.bookstore.domain.po.User;
import com.bookstore.domain.vo.user.UserProfileVO;
import com.bookstore.mapper.UserMapper;
import com.bookstore.service.impl.UserServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceProfileTest {

    @Mock UserMapper userMapper;
    @InjectMocks UserServiceImpl userService;

    @Test
    void getProfile_returns_masked_phone_and_full_avatar_url() {
        ReflectionTestUtils.setField(userService, "ossPrefix", "https://oss.example.com/");

        User u = new User();
        u.setId(1L);
        u.setUsername("alice");
        u.setPhone("13812345678");
        u.setNickname("Alice");
        u.setAvatarKey("avatars/1.jpg");
        u.setRole("USER");
        when(userMapper.selectById(1L)).thenReturn(u);

        UserProfileVO vo = userService.getProfile(1L);

        assertThat(vo.getPhone()).isEqualTo("138****5678");
        assertThat(vo.getAvatarUrl()).isEqualTo("https://oss.example.com/avatars/1.jpg");
        assertThat(vo.getUsername()).isEqualTo("alice");
    }

    @Test
    void getProfile_throws_when_not_found() {
        when(userMapper.selectById(99L)).thenReturn(null);
        assertThatThrownBy(() -> userService.getProfile(99L))
            .isInstanceOf(BusinessException.class);
    }
}
