package com.bookstore.domain.vo.coupon;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class CouponTemplateVO {

    private Long id;
    private String name;
    private String type;
    private BigDecimal threshold;
    private BigDecimal discountValue;
    private Integer totalCount;
    private Integer claimedCount;
    private LocalDateTime validFrom;
    private LocalDateTime validTo;
    private String status;
    private String description;
    private LocalDateTime createTime;
}
