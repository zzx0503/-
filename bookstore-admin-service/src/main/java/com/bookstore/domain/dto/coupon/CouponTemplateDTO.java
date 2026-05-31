package com.bookstore.domain.dto.coupon;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class CouponTemplateDTO {

    @NotBlank
    @Size(max = 100)
    private String name;

    @NotBlank
    private String type;

    @NotNull
    @DecimalMin("0.00")
    private BigDecimal threshold;

    @NotNull
    @DecimalMin("0.00")
    private BigDecimal discountValue;

    @NotNull
    private Integer totalCount;

    @NotNull
    private LocalDateTime validFrom;

    @NotNull
    private LocalDateTime validTo;

    @Size(max = 255)
    private String description;
}
