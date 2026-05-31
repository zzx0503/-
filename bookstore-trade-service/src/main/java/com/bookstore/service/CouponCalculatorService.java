package com.bookstore.service;

import com.bookstore.domain.po.CouponTemplate;
import com.bookstore.domain.po.UserCoupon;
import com.bookstore.domain.vo.coupon.CouponSelectionVO;

import java.math.BigDecimal;
import java.util.List;

public interface CouponCalculatorService {

    BigDecimal calcDiscount(BigDecimal totalAmount, CouponTemplate template);

    boolean isUsable(BigDecimal totalAmount, CouponTemplate template);

    CouponSelectionVO findBest(Long userId, List<Long> cartItemIds);

    CouponSelectionVO findBestByAmount(Long userId, BigDecimal totalAmount);

    record CouponWithTemplate(UserCoupon coupon, CouponTemplate template) {
    }
}
