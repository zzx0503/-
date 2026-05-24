package com.bookstore.domain.dto.seckill;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SeckillBuyDTO {

    @NotNull
    private Long activityId;

    @NotNull
    private Long addressId;
}
