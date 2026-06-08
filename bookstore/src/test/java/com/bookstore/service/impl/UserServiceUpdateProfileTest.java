package com.bookstore.service.impl;

import com.bookstore.domain.dto.user.UpdateProfileDTO;
import com.bookstore.domain.po.User;
import com.bookstore.domain.vo.user.UserProfileVO;
import com.bookstore.mapper.UserMapper;
import com.bookstore.service.impl.UserServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceUpdateProfileTest {

    @Mock UserMapper userMapper;
    @InjectMocks UserServiceImpl userService;

    @Test
    void updateProfile_only_writes_allowed_fields() {
        User existing = new User();
        existing.setId(1L);
        existing.setUsername("alice");
        existing.setPhone("13812345678");
        existing.setNickname("Old");
        existing.setRole("USER");
        when(userMapper.selectById(1L)).thenReturn(existing);

        UpdateProfileDTO dto = new UpdateProfileDTO();
        dto.setNickname("New Alice");
        dto.setGender(1);
        dto.setBirthday(LocalDate.of(2000, 1, 1));

        UserProfileVO vo = userService.updateProfile(1L, dto);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userMapper).updateById(captor.capture());
        User updated = captor.getValue();

        assertThat(updated.getNickname()).isEqualTo("New Alice");
        assertThat(updated.getGender()).isEqualTo(1);
        assertThat(updated.getBirthday()).isEqualTo(LocalDate.of(2000, 1, 1));
        assertThat(updated.getUsername()).isNull();
        assertThat(updated.getPhone()).isNull();

        assertThat(vo.getNickname()).isEqualTo("New Alice");
    }
}
