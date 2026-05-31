package com.bookstore.domain.dto.order;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class CreateOrderDTO {

    @NotEmpty
    private List<Long> cartItemIds;

    @NotNull
    private Long addressId;

    private Long userCouponId;

    private String remark;
}
