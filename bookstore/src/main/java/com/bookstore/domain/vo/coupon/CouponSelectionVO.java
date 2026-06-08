package com.bookstore.domain.vo.coupon;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class CouponSelectionVO {

    private BigDecimal totalAmount;
    private UserCouponVO bestCoupon;
    private BigDecimal discountAmount;
    private BigDecimal payAmount;
    private List<UserCouponVO> usableCoupons;
}
