package com.bookstore.exception;

import com.bookstore.response.Result;
import com.bookstore.response.ResultCode;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

/**
 * 全局异常处理器
 * <p>
 * 使用 @RestControllerAdvice 拦截整个应用中抛出的异常，
 * 并将其统一转换为 Result<T> 格式返回给前端，确保响应结构的一致性。
 * </p>
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 处理业务逻辑异常 (BusinessException)
     * <p>
     * 当 Service 层抛出预定义的业务异常时触发，记录警告日志并返回对应的错误码和消息。
     * </p>
     * @param ex 捕获到的业务异常
     * @param req 当前 HTTP 请求对象，用于记录请求路径
     * @return 封装了错误信息的 Result 对象
     */
    @ExceptionHandler(BusinessException.class)
    public Result<Void> handleBusiness(BusinessException ex, HttpServletRequest req) {
        log.warn("biz error [{}] {} -> {}", req.getMethod(), req.getRequestURI(), ex.getMessage());
        return Result.fail(ex.getResultCode(), ex.getMessage());
    }

    /**
     * 处理参数校验异常 (@RequestBody 校验失败)
     * <p>
     * 当 Controller 方法参数上的 @Valid 或 @Validated 校验不通过时触发。
     * 会将所有字段的错误信息拼接成字符串返回。
     * </p>
     * @param ex 方法参数校验异常
     * @return 包含具体字段错误提示的 Result 对象
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<Void> handleValid(MethodArgumentNotValidException ex) {
        String msg = ex.getBindingResult().getFieldErrors().stream()
            .map(fe -> fe.getField() + " " + fe.getDefaultMessage())
            .collect(Collectors.joining("; "));
        return Result.fail(ResultCode.PARAM_INVALID, msg);
    }

    /**
     * 处理表单绑定异常 (BindException)
     * <p>
     * 通常发生在 GET 请求参数绑定到对象时校验失败的情况。
     * </p>
     *
     * @param ex 绑定异常
     * @return 包含错误提示的 Result 对象
     */
    @ExceptionHandler(BindException.class)
    public Result<Void> handleBind(BindException ex) {
        String msg = ex.getBindingResult().getFieldErrors().stream()
            .map(FieldError::getDefaultMessage)
            .collect(Collectors.joining("; "));
        return Result.fail(ResultCode.PARAM_INVALID, msg);
    }

    /**
     * 兜底处理：处理所有未被捕获的未知异常
     * <p>
     * 记录完整的错误堆栈以便排查问题，并向用户返回通用的“服务器繁忙”提示，
     * 避免将内部实现细节（如类名、SQL语句）暴露给前端。
     * </p>
     *
     * @param ex 捕获到的通用异常
     * @param req 当前 HTTP 请求对象
     * @return 统一的服务器错误响应
     */
    @ExceptionHandler(Exception.class)
    public Result<Void> handleAny(Exception ex, HttpServletRequest req) {
        log.error("unexpected error [{}] {}", req.getMethod(), req.getRequestURI(), ex);
        return Result.fail(ResultCode.SERVER_ERROR, "服务器繁忙,请稍后再试");
    }
}
