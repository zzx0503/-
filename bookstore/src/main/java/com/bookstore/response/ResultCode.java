package com.bookstore.response;

import lombok.Getter;

@Getter
public enum ResultCode {

    SUCCESS(200, "OK"),
    PARAM_INVALID(400, "参数错误"),
    UNAUTHORIZED(401, "未登录或登录已过期"),
    FORBIDDEN(403, "权限不足"),
    NOT_FOUND(404, "资源不存在"),
    SERVER_ERROR(500, "服务器内部错误"),

    BIZ_ERROR(1000, "业务错误"),
    USER_NOT_FOUND(1001, "用户不存在"),
    USER_PASSWORD_WRONG(1002, "密码错误"),
    USER_BANNED(1003, "账号已被封禁"),
    USERNAME_DUPLICATED(1004, "用户名已被占用"),
    TOKEN_INVALID(1005, "Token 无效"),
    TOKEN_EXPIRED(1006, "Token 已过期"),
    REFRESH_TOKEN_INVALID(1007, "刷新令牌无效"),
    RATE_LIMIT(1008, "请求过于频繁,稍后再试"),
    USERNAME_TAKEN(1010, "用户名已被注册"),
    PHONE_TAKEN(1011, "手机号已被注册"),
    ACCOUNT_NOT_FOUND(1012, "账号不存在"),
    PASSWORD_WRONG(1013, "密码错误"),
    ACCOUNT_DISABLED(1014, "账号已禁用"),
    TOKEN_TYPE_MISMATCH(1015, "Token 类型不正确"),
    NEW_PASSWORD_SAME_AS_OLD(1016, "新密码与原密码相同"),

    CATEGORY_NOT_FOUND(1101, "分类不存在"),
    BOOK_NOT_FOUND(1102, "图书不存在"),
    CART_ITEM_NOT_FOUND(1103, "购物车项不存在"),
    ORDER_NOT_FOUND(1104, "订单不存在"),
    STOCK_INSUFFICIENT(1105, "库存不足"),
    ORDER_STATUS_INVALID(1106, "订单状态不允许此操作"),
    FAVORITE_ALREADY_EXISTS(1107, "已收藏该图书"),
    REVIEW_ALREADY_EXISTS(1108, "该订单已评价"),

    COUPON_TEMPLATE_NOT_FOUND(1201, "优惠券模板不存在"),
    COUPON_NOT_AVAILABLE(1202, "优惠券不可领取"),
    COUPON_OUT_OF_STOCK(1203, "优惠券已抢光"),
    COUPON_NOT_FOUND(1204, "优惠券不存在"),
    COUPON_NOT_USABLE(1205, "优惠券不可使用"),
    COUPON_THRESHOLD_NOT_MET(1206, "未达到优惠券使用门槛"),
    COUPON_LOCKED_BY_OTHER(1207, "优惠券已被锁定"),

    SECKILL_NOT_RUNNING(1301, "秒杀活动未进行中"),
    SECKILL_OUT_OF_STOCK(1302, "已售罄"),
    SECKILL_LIMIT_REACHED(1303, "您已抢购过该活动"),
    SECKILL_ORDER_NOT_FOUND(1304, "秒杀订单不存在"),
    SECKILL_ORDER_EXPIRED(1305, "秒杀订单已超时"),
    SECKILL_QUEUE_FULL(1306, "秒杀队列已满，请稍后再试"),

    AI_UNAVAILABLE(1401, "AI 服务暂不可用"),
    AI_SESSION_NOT_FOUND(1402, "会话不存在");

    private final int code;
    private final String defaultMsg;

    ResultCode(int code, String defaultMsg) {
        this.code = code;
        this.defaultMsg = defaultMsg;
    }
}
