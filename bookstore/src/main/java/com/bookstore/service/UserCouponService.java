package com.bookstore.service;

import com.bookstore.domain.po.UserCoupon;
import com.bookstore.domain.vo.coupon.ClaimResultVO;
import com.bookstore.domain.vo.coupon.UserCouponVO;

import java.util.List;

public interface UserCouponService {

    ClaimResultVO claim(Long userId, Long templateId);

    List<UserCouponVO> listMine(Long userId, String status);

    UserCoupon lockForOrder(Long userId, Long userCouponId, String orderNo);

    void useForOrder(Long userId, Long userCouponId, String orderNo);

    void releaseForOrder(Long userId, Long userCouponId, String orderNo);

    int autoReleaseStuckLocked();

    int autoExpire();
}
