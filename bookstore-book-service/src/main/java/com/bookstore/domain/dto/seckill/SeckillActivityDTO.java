package com.bookstore.domain.dto.seckill;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class SeckillActivityDTO {

    @NotNull
    private Long bookId;

    @NotNull
    @DecimalMin("0.01")
    private BigDecimal seckillPrice;

    @NotNull
    @Min(1)
    private Integer totalStock;

    @Min(1)
    private Integer perUserLimit;

    @NotNull
    private LocalDateTime startTime;

    @NotNull
    private LocalDateTime endTime;

    @Size(max = 100)
    private String title;
}
