package com.bookstore.domain.vo.coupon;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class UserCouponVO {

    private Long id;
    private Long templateId;
    private String name;
    private String type;
    private BigDecimal threshold;
    private BigDecimal discountValue;
    private String code;
    private String status;
    private String lockedOrderNo;
    private String usedOrderNo;
    private LocalDateTime usedAt;
    private LocalDateTime expireTime;
    private LocalDateTime createTime;
    private String description;
}
