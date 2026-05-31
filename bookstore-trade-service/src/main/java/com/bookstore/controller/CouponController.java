package com.bookstore.controller;

import com.bookstore.anno.LoginRequired;
import com.bookstore.context.UserContext;
import com.bookstore.domain.dto.coupon.CalcCouponDTO;
import com.bookstore.domain.vo.coupon.AvailableCouponVO;
import com.bookstore.domain.vo.coupon.ClaimResultVO;
import com.bookstore.domain.vo.coupon.CouponSelectionVO;
import com.bookstore.domain.vo.coupon.UserCouponVO;
import com.bookstore.response.Result;
import com.bookstore.service.CouponCalculatorService;
import com.bookstore.service.CouponTemplateService;
import com.bookstore.service.UserCouponService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "优惠券", description = "用户端优惠券")
@RestController
@RequestMapping("/api/coupons")
@RequiredArgsConstructor
public class CouponController {

    private final CouponTemplateService couponTemplateService;
    private final UserCouponService userCouponService;
    private final CouponCalculatorService couponCalculatorService;

    @GetMapping("/available")
    public Result<List<AvailableCouponVO>> available() {
        Long userId = UserContext.get() == null ? null : UserContext.get().getUserId();
        return Result.success(couponTemplateService.listAvailable(userId));
    }

    @PostMapping("/{templateId}/claim")
    @LoginRequired
    public Result<ClaimResultVO> claim(@PathVariable Long templateId) {
        return Result.success(userCouponService.claim(UserContext.requireUserId(), templateId));
    }

    @GetMapping("/mine")
    @LoginRequired
    public Result<List<UserCouponVO>> mine(@RequestParam(required = false) String status) {
        return Result.success(userCouponService.listMine(UserContext.requireUserId(), status));
    }

    @PostMapping("/calc")
    @LoginRequired
    public Result<CouponSelectionVO> calc(@Valid @RequestBody CalcCouponDTO dto) {
        return Result.success(couponCalculatorService.findBest(UserContext.requireUserId(), dto.getCartItemIds()));
    }
}
