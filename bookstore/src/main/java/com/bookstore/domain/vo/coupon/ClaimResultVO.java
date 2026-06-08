package com.bookstore.domain.vo.coupon;

import lombok.Data;

@Data
public class ClaimResultVO {

    private Long userCouponId;
    private String code;
    private String status;
}
