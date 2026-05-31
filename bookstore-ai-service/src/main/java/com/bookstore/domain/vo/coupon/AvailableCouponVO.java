package com.bookstore.domain.vo.coupon;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class AvailableCouponVO {

    private Long templateId;
    private String name;
    private String type;
    private BigDecimal threshold;
    private BigDecimal discountValue;
    private Integer remainingCount;
    private LocalDateTime validFrom;
    private LocalDateTime validTo;
    private String description;
    private Boolean claimed;
}
