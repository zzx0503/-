package com.bookstore.controller;

import com.bookstore.response.Result;
import com.bookstore.domain.dto.auth.LoginDTO;
import com.bookstore.domain.dto.auth.RefreshTokenDTO;
import com.bookstore.domain.dto.auth.RegisterDTO;
import com.bookstore.domain.vo.auth.TokenVO;
import com.bookstore.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 认证控制器
 * 提供用户注册、登录、Token刷新和登出功能
 */
@Tag(name = "认证", description = "登录、注册、刷新与登出")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * 用户注册
     * @param dto 注册信息（用户名、密码等）
     * @return JWT Token对（Access Token + Refresh Token）
     */
    @Operation(summary = "用户注册")
    @PostMapping("/register")
    public Result<TokenVO> register(@Valid @RequestBody RegisterDTO dto) {
        return Result.success(authService.register(dto));
    }

    /**
     * 用户登录
     * @param dto 登录信息（用户名、密码）
     * @return JWT Token对
     */
    @Operation(summary = "用户登录")
    @PostMapping("/login")
    public Result<TokenVO> login(@Valid @RequestBody LoginDTO dto) {
        return Result.success(authService.login(dto));
    }

    /**
     * 刷新 Access Token
     * @param dto 包含有效的 Refresh Token
     * @return 新的 JWT Token对
     */
    @Operation(summary = "刷新 Access Token")
    @PostMapping("/refresh")
    public Result<TokenVO> refresh(@Valid @RequestBody RefreshTokenDTO dto) {
        return Result.success(authService.refresh(dto));
    }

    /**
     * 用户登出
     * 将当前 Access Token 加入黑名单，使其立即失效
     * @param request HTTP请求对象，用于提取 Token
     * @return 操作结果
     */
    @Operation(summary = "用户登出")
    @PostMapping("/logout")
    public Result<Void> logout(HttpServletRequest request) {
        authService.logout(extractBearer(request));
        return Result.success();
    }

    /**
     * 从 Authorization Header 中提取 Bearer Token
     * @param request HTTP请求对象
     * @return Token字符串，如果不存在则返回null
     */
    private String extractBearer(HttpServletRequest request) {
        String h = request.getHeader("Authorization");
        if (h != null && h.startsWith("Bearer ")) {
            return h.substring(7);
        }
        return null;
    }
}
