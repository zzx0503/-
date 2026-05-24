package com.bookstore.service.impl;

import com.bookstore.exception.BusinessException;
import com.bookstore.response.ResultCode;
import com.bookstore.utils.PasswordUtil;
import com.bookstore.domain.dto.user.ChangePasswordDTO;
import com.bookstore.domain.dto.user.UpdateProfileDTO;
import com.bookstore.domain.po.User;
import com.bookstore.domain.vo.user.UserProfileVO;
import com.bookstore.mapper.UserMapper;
import com.bookstore.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;

    @Value("${bookstore.oss.public-prefix:}")
    private String ossPrefix;

    @Override
    public UserProfileVO getProfile(Long userId) {
        User u = userMapper.selectById(userId);
        if (u == null) {
            throw new BusinessException(ResultCode.ACCOUNT_NOT_FOUND);
        }
        return toVO(u);
    }

    @Override
    public UserProfileVO updateProfile(Long userId, UpdateProfileDTO dto) {
        User existing = userMapper.selectById(userId);
        if (existing == null) {
            throw new BusinessException(ResultCode.ACCOUNT_NOT_FOUND);
        }

        User patch = new User();
        patch.setId(userId);
        patch.setNickname(dto.getNickname());
        patch.setGender(dto.getGender());
        patch.setBirthday(dto.getBirthday());
        userMapper.updateById(patch);

        if (dto.getNickname() != null) existing.setNickname(dto.getNickname());
        if (dto.getGender() != null) existing.setGender(dto.getGender());
        if (dto.getBirthday() != null) existing.setBirthday(dto.getBirthday());
        return toVO(existing);
    }

    @Override
    public void changePassword(Long userId, ChangePasswordDTO dto) {
        User u = userMapper.selectById(userId);
        if (u == null) {
            throw new BusinessException(ResultCode.ACCOUNT_NOT_FOUND);
        }
        if (!PasswordUtil.matches(dto.getOldPassword(), u.getPasswordHash())) {
            throw new BusinessException(ResultCode.PASSWORD_WRONG);
        }
        if (dto.getOldPassword().equals(dto.getNewPassword())) {
            throw new BusinessException(ResultCode.NEW_PASSWORD_SAME_AS_OLD);
        }

        User patch = new User();
        patch.setId(userId);
        patch.setPasswordHash(PasswordUtil.encode(dto.getNewPassword()));
        userMapper.updateById(patch);
    }

    @Override
    public UserProfileVO updateAvatar(Long userId, String avatarKey) {
        User existing = userMapper.selectById(userId);
        if (existing == null) {
            throw new BusinessException(ResultCode.ACCOUNT_NOT_FOUND);
        }
        User patch = new User();
        patch.setId(userId);
        patch.setAvatarKey(avatarKey);
        userMapper.updateById(patch);

        existing.setAvatarKey(avatarKey);
        return toVO(existing);
    }

    UserProfileVO toVO(User u) {
        UserProfileVO vo = new UserProfileVO();
        vo.setId(u.getId());
        vo.setUsername(u.getUsername());
        vo.setPhone(maskPhone(u.getPhone()));
        vo.setEmail(u.getEmail());
        vo.setNickname(u.getNickname());
        vo.setAvatarUrl(u.getAvatarKey() == null ? null : ossPrefix + u.getAvatarKey());
        vo.setGender(u.getGender());
        vo.setBirthday(u.getBirthday());
        vo.setRole(u.getRole());
        return vo;
    }

    private String maskPhone(String phone) {
        if (phone == null || phone.length() != 11) return phone;
        return phone.substring(0, 3) + "****" + phone.substring(7);
    }
}
