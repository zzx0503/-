package com.bookstore.service.impl;

import com.bookstore.exception.BusinessException;
import com.bookstore.response.ResultCode;
import com.bookstore.utils.PasswordUtil;
import com.bookstore.domain.dto.user.ChangePasswordDTO;
import com.bookstore.domain.po.User;
import com.bookstore.mapper.UserMapper;
import com.bookstore.service.impl.UserServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceChangePasswordTest {

    @Mock UserMapper userMapper;
    @InjectMocks UserServiceImpl userService;

    @Test
    void changePassword_succeeds() {
        User existing = new User();
        existing.setId(1L);
        existing.setPasswordHash(PasswordUtil.encode("oldpass"));
        when(userMapper.selectById(1L)).thenReturn(existing);

        ChangePasswordDTO dto = new ChangePasswordDTO();
        dto.setOldPassword("oldpass");
        dto.setNewPassword("newpass");

        userService.changePassword(1L, dto);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userMapper).updateById(captor.capture());
        User patch = captor.getValue();

        assertThat(patch.getId()).isEqualTo(1L);
        assertThat(patch.getPasswordHash()).isNotNull();
        assertThat(PasswordUtil.matches("newpass", patch.getPasswordHash())).isTrue();
        assertThat(patch.getUsername()).isNull();
        assertThat(patch.getPhone()).isNull();
    }

    @Test
    void changePassword_rejects_wrong_old() {
        User existing = new User();
        existing.setId(1L);
        existing.setPasswordHash(PasswordUtil.encode("oldpass"));
        when(userMapper.selectById(1L)).thenReturn(existing);

        ChangePasswordDTO dto = new ChangePasswordDTO();
        dto.setOldPassword("wrongpass");
        dto.setNewPassword("newpass");

        assertThatThrownBy(() -> userService.changePassword(1L, dto))
            .isInstanceOf(BusinessException.class)
            .extracting("resultCode").isEqualTo(ResultCode.PASSWORD_WRONG);

        verify(userMapper, never()).updateById(org.mockito.ArgumentMatchers.any(User.class));
    }

    @Test
    void changePassword_rejects_same_password() {
        User existing = new User();
        existing.setId(1L);
        existing.setPasswordHash(PasswordUtil.encode("samepass"));
        when(userMapper.selectById(1L)).thenReturn(existing);

        ChangePasswordDTO dto = new ChangePasswordDTO();
        dto.setOldPassword("samepass");
        dto.setNewPassword("samepass");

        assertThatThrownBy(() -> userService.changePassword(1L, dto))
            .isInstanceOf(BusinessException.class)
            .extracting("resultCode").isEqualTo(ResultCode.NEW_PASSWORD_SAME_AS_OLD);

        verify(userMapper, never()).updateById(org.mockito.ArgumentMatchers.any(User.class));
    }
}
