package com.bookstore.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import lombok.Data;

/**
 * 统一 API 响应封装类
 * <p>
 * 所有 Controller 的返回值都使用此类进行包装，确保前后端交互格式统一。
 * 前端只需根据 code 字段判断请求是否成功，msg 用于展示提示信息，data 承载业务数据。
 * </p>
 *
 * @param <T> 响应数据的泛型类型
 */
@Data
@JsonInclude(JsonInclude.Include.ALWAYS)
public class Result<T> {

    /** 响应状态码，200 表示成功，其他值表示不同类型的错误 */
    private int code;
    /** 响应消息，用于向用户展示提示信息或错误说明 */
    private String msg;
    /** 业务数据载体，成功时返回具体数据，失败时为 null */
    private T data;
    /** 响应耗时（毫秒），仅 AI 搜索等耗时接口返回 */
    @JsonInclude(Include.NON_NULL)
    private Long durationMs;
    /**
     * 私有构造方法，防止外部直接实例化
     * 统一通过静态工厂方法创建对象
     */
    private Result() {}
    /**
     * 构建成功的响应（带数据）
     * @param data 业务数据
     * @param <T> 数据类型
     * @return 封装好的成功响应对象，code=200, msg="OK"
     */
    public static <T> Result<T> success(T data) {
        Result<T> r = new Result<>();
        r.code = ResultCode.SUCCESS.getCode();
        r.msg = ResultCode.SUCCESS.getDefaultMsg();
        r.data = data;
        return r;
    }

    /**
     * 构建成功的响应（带数据及耗时）
     * @param data 业务数据
     * @param durationMs 接口处理耗时（毫秒）
     * @param <T> 数据类型
     * @return 封装好的成功响应对象，code=200, msg="OK", durationMs=处理耗时
     */
    public static <T> Result<T> success(T data, Long durationMs) {
        Result<T> r = success(data);
        r.durationMs = durationMs;
        return r;
    }

    /**
     * 构建成功的响应（无数据）
     * <p>
     * 适用于删除、更新等不需要返回实体数据的操作
     * </p>
     * @param <T> 数据类型
     * @return 封装好的成功响应对象，code=200, msg="OK", data=null
     */
    public static <T> Result<T> success() {
        return success(null);
    }

    /**
     * 构建失败的响应（自定义错误消息）
     * @param rc 错误码枚举，决定响应的 code 值
     * @param msg 自定义错误提示信息，覆盖默认消息
     * @param <T> 数据类型
     * @return 封装好的失败响应对象，data=null
     */
    public static <T> Result<T> fail(ResultCode rc, String msg) {
        Result<T> r = new Result<>();
        r.code = rc.getCode();
        r.msg = msg;
        return r;
    }

    /**
     * 构建失败的响应（使用默认错误消息）
     * <p>
     * 直接使用 ResultCode 中预定义的默认消息
     * </p>
     * @param rc 错误码枚举
     * @param <T> 数据类型
     * @return 封装好的失败响应对象，data=null
     */
    public static <T> Result<T> fail(ResultCode rc) {
        return fail(rc, rc.getDefaultMsg());
    }
}
