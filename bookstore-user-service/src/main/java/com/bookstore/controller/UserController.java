package com.bookstore.controller;

import com.bookstore.anno.LoginRequired;
import com.bookstore.config.AvatarProperties;
import com.bookstore.context.UserContext;
import com.bookstore.response.Result;
import com.bookstore.domain.dto.user.ChangePasswordDTO;
import com.bookstore.domain.dto.user.UpdateAvatarDTO;
import com.bookstore.domain.dto.user.UpdateProfileDTO;
import com.bookstore.domain.vo.user.AvatarPresetVO;
import com.bookstore.domain.vo.user.UserProfileVO;
import com.bookstore.service.OSSService;
import com.bookstore.service.UserService;
import com.bookstore.utils.OssUrlBuilder;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Tag(name = "用户", description = "当前用户资料、密码与头像")
@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
@LoginRequired
public class UserController {

    private final UserService userService;
    private final OSSService ossService;
    private final OssUrlBuilder ossUrlBuilder;
    private final AvatarProperties avatarProperties;

    @GetMapping("/me")
    public Result<UserProfileVO> me() {
        return Result.success(userService.getProfile(UserContext.requireUserId()));
    }

    @PutMapping("/me")
    public Result<UserProfileVO> updateMe(@Valid @RequestBody UpdateProfileDTO dto) {
        return Result.success(userService.updateProfile(UserContext.requireUserId(), dto));
    }

    @PutMapping("/me/password")
    public Result<Void> changePassword(@Valid @RequestBody ChangePasswordDTO dto) {
        userService.changePassword(UserContext.requireUserId(), dto);
        return Result.success();
    }

    @PutMapping("/me/avatar")
    public Result<UserProfileVO> updateAvatar(@Valid @RequestBody UpdateAvatarDTO dto) {
        return Result.success(userService.updateAvatar(UserContext.requireUserId(), dto.getAvatarKey()));
    }

    @PostMapping("/me/avatar/upload")
    public Result<UserProfileVO> uploadAvatar(@RequestParam("file") MultipartFile file) {
        String avatarKey = ossService.uploadAvatar(UserContext.requireUserId(), file);
        return Result.success(userService.updateAvatar(UserContext.requireUserId(), avatarKey));
    }

    @GetMapping("/me/avatar/presets")
    public Result<List<AvatarPresetVO>> avatarPresets() {
        List<AvatarPresetVO> list = avatarProperties.getPresets().stream()
            .map(key -> new AvatarPresetVO(key, ossUrlBuilder.toFullUrl(key)))
            .toList();
        return Result.success(list);
    }
}
