package com.bookstore.domain.dto.cart;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CartItemFormDTO {

    @NotNull
    private Long bookId;

    @NotNull
    @Min(1)
    private Integer quantity;
}
