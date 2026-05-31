package com.bookstore.exception;

import com.bookstore.response.ResultCode;
import lombok.Getter;

/**
 * 业务逻辑异常
 * <p>
 * 用于在 Service 层处理业务规则校验失败时抛出。
 * 该异常携带 ResultCode，方便全局处理器将其转换为统一的响应格式。
 * </p>
 */
@Getter
public class BusinessException extends RuntimeException {

    /** 关联的错误码枚举 */
    private final ResultCode resultCode;

    /**
     * 使用预定义的错误码和默认消息构造异常
     *
     * @param resultCode 错误码枚举
     */
    public BusinessException(ResultCode resultCode) {
        super(resultCode.getDefaultMsg());
        this.resultCode = resultCode;
    }

    /**
     * 使用预定义的错误码和自定义消息构造异常
     *
     * @param resultCode 错误码枚举
     * @param msg 自定义的错误提示信息
     */
    public BusinessException(ResultCode resultCode, String msg) {
        super(msg);
        this.resultCode = resultCode;
    }
}
