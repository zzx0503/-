package com.bookstore.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bookstore.exception.BusinessException;
import com.bookstore.response.ResultCode;
import com.bookstore.utils.JwtUtil;
import com.bookstore.utils.PasswordUtil;
import com.bookstore.domain.dto.auth.LoginDTO;
import com.bookstore.domain.dto.auth.RefreshTokenDTO;
import com.bookstore.domain.dto.auth.RegisterDTO;
import com.bookstore.domain.po.User;
import com.bookstore.domain.vo.auth.TokenVO;
import com.bookstore.domain.vo.auth.UserBriefVO;
import com.bookstore.mapper.UserMapper;
import com.bookstore.service.AuthService;
import com.bookstore.service.TokenBlacklistService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 认证服务实现类
 * 处理用户注册、登录、Token刷新和登出等核心认证逻辑
 */
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserMapper userMapper;
    private final JwtUtil jwtUtil;
    private final TokenBlacklistService blacklist;

    /**
     * 用户注册
     * 1. 检查用户名和手机号是否已被占用
     * 2. 对密码进行 BCrypt 加密
     * 3. 创建用户记录并生成 JWT Token
     * @param dto 注册信息
     * @return JWT Token对
     * @throws BusinessException 用户名或手机号已存在时抛出
     */
    @Override
    @Transactional
    public TokenVO register(RegisterDTO dto) {
        // 检查用户名是否已存在
        Long usernameCount = userMapper.selectCount(
            new LambdaQueryWrapper<User>().eq(User::getUsername, dto.getUsername())
        );
        if (usernameCount > 0) {
            throw new BusinessException(ResultCode.USERNAME_TAKEN);
        }

        // 检查手机号是否已存在
        Long phoneCount = userMapper.selectCount(
            new LambdaQueryWrapper<User>().eq(User::getPhone, dto.getPhone())
        );
        if (phoneCount > 0) {
            throw new BusinessException(ResultCode.PHONE_TAKEN);
        }

        // 创建新用户
        User user = new User();
        user.setUsername(dto.getUsername());
        user.setPhone(dto.getPhone());
        user.setPasswordHash(PasswordUtil.encode(dto.getPassword()));  // BCrypt 加密
        user.setNickname(dto.getUsername());
        user.setRole("USER");  // 默认角色为普通用户
        user.setStatus(1);  // 默认启用状态
        userMapper.insert(user);

        return buildTokenVO(user);
    }

    /**
     * 用户登录
     * 支持用户名或手机号登录
     * 1. 查找用户（用户名或手机号）
     * 2. 验证账号状态
     * 3. 校验密码
     * 4. 生成 JWT Token
     * @param dto 登录信息（账号、密码）
     * @return JWT Token对
     * @throws BusinessException 账号不存在、已禁用或密码错误时抛出
     */
    @Override
    public TokenVO login(LoginDTO dto) {
        // 支持用户名或手机号登录
        User user = userMapper.selectOne(
            new LambdaQueryWrapper<User>()
                .and(w -> w.eq(User::getUsername, dto.getAccount())
                           .or()
                           .eq(User::getPhone, dto.getAccount()))
        );
        if (user == null) {
            throw new BusinessException(ResultCode.ACCOUNT_NOT_FOUND);
        }
        // 检查账号是否被禁用
        if (user.getStatus() == null || user.getStatus() == 0) {
            throw new BusinessException(ResultCode.ACCOUNT_DISABLED);
        }
        // 验证密码
        if (!PasswordUtil.matches(dto.getPassword(), user.getPasswordHash())) {
            throw new BusinessException(ResultCode.PASSWORD_WRONG);
        }
        return buildTokenVO(user);
    }

    /**
     * 刷新 Access Token
     * 1. 解析 Refresh Token 并验证类型
     * 2. 检查 Token 是否在黑名单中
     * 3. 验证用户状态
     * 4. 将旧 Refresh Token 加入黑名单
     * 5. 颁发新的 Token 对
     * @param dto 包含 Refresh Token
     * @return 新的 JWT Token对
     * @throws BusinessException Token无效或用户已禁用时抛出
     */
    @Override
    public TokenVO refresh(RefreshTokenDTO dto) {
        // 解析 Refresh Token
        JwtUtil.UserClaims claims = jwtUtil.parse(dto.getRefreshToken());

        // 验证 Token 类型必须是 refresh
        String type = jwtUtil.getTokenType(dto.getRefreshToken());
        if (!"refresh".equals(type)) {
            throw new BusinessException(ResultCode.TOKEN_TYPE_MISMATCH);
        }

        // 检查是否在黑名单中
        if (blacklist.isRevoked(claims.jti())) {
            throw new BusinessException(ResultCode.TOKEN_INVALID);
        }

        // 验证用户是否存在且状态正常
        User user = userMapper.selectById(claims.userId());
        if (user == null) {
            throw new BusinessException(ResultCode.ACCOUNT_NOT_FOUND);
        }
        if (user.getStatus() == null || user.getStatus() == 0) {
            throw new BusinessException(ResultCode.ACCOUNT_DISABLED);
        }

        // 将旧的 Refresh Token 加入黑名单，防止重用
        blacklist.revoke(claims.jti(), claims.expiresAtEpochMs());

        return buildTokenVO(user);
    }

    /**
     * 用户登出
     * 将 Access Token 加入黑名单，使其立即失效
     * @param accessToken 需要撤销的 Access Token
     */
    @Override
    public void logout(String accessToken) {
        if (accessToken == null || accessToken.isBlank()) {
            return;
        }
        try {
            JwtUtil.UserClaims claims = jwtUtil.parse(accessToken);
            blacklist.revoke(claims.jti(), claims.expiresAtEpochMs());
        } catch (Exception ignore) {
            // 忽略解析失败的 Token
        }
    }

    /**
     * 构建 Token 响应对象
     * 生成 Access Token 和 Refresh Token，并封装用户简要信息
     * @param user 用户对象
     * @return TokenVO 包含 Token 和用户信息
     */
    private TokenVO buildTokenVO(User user) {
        String access = jwtUtil.issueAccessToken(user.getId(), user.getUsername(), user.getRole());
        String refresh = jwtUtil.issueRefreshToken(user.getId(), user.getUsername(), user.getRole());
        UserBriefVO brief = new UserBriefVO();
        brief.setId(user.getId());
        brief.setUsername(user.getUsername());
        brief.setNickname(user.getNickname());
        brief.setRole(user.getRole());
        return new TokenVO(access, refresh, jwtUtil.getAccessTtlSeconds(), brief);
    }
}
