package com.bookstore.exception;

import com.bookstore.response.ResultCode;

/**
 * 认证授权异常
 * <p>
 * 继承自 BusinessException，专门用于处理登录、Token 校验、权限不足等安全相关的业务异常。
 * </p>
 */
public class AuthException extends BusinessException {
    
    /**
     * 使用预定义的错误码构造认证异常
     *
     * @param resultCode 错误码枚举（如 UNAUTHORIZED, FORBIDDEN）
     */
    public AuthException(ResultCode resultCode) {
        super(resultCode);
    }

    /**
     * 使用预定义的错误码和自定义消息构造认证异常
     *
     * @param resultCode 错误码枚举
     * @param msg 自定义的错误提示信息
     */
    public AuthException(ResultCode resultCode, String msg) {
        super(resultCode, msg);
    }
}
