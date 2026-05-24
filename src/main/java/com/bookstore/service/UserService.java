package com.bookstore.service;

import com.bookstore.domain.dto.user.ChangePasswordDTO;
import com.bookstore.domain.dto.user.UpdateProfileDTO;
import com.bookstore.domain.vo.user.UserProfileVO;

public interface UserService {

    UserProfileVO getProfile(Long userId);

    UserProfileVO updateProfile(Long userId, UpdateProfileDTO dto);

    void changePassword(Long userId, ChangePasswordDTO dto);

    UserProfileVO updateAvatar(Long userId, String avatarKey);
}
