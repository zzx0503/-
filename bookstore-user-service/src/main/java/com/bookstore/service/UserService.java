package com.bookstore.service;

import com.bookstore.domain.dto.user.ChangePasswordDTO;
import com.bookstore.domain.dto.user.UpdateProfileDTO;
import com.bookstore.api.user.dto.UserProfileDTO;

public interface UserService {

    UserProfileDTO getProfile(Long userId);

    UserProfileDTO updateProfile(Long userId, UpdateProfileDTO dto);

    void changePassword(Long userId, ChangePasswordDTO dto);

    UserProfileDTO updateAvatar(Long userId, String avatarKey);
}
