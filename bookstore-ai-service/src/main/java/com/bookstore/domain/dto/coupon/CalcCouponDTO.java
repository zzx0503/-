package com.bookstore.domain.dto.coupon;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class CalcCouponDTO {

    @NotEmpty(message = "至少包含一项购物车商品")
    private List<Long> cartItemIds;
}
