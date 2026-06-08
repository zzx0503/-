package com.bookstore.domain.dto.order;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class PayOrderDTO {

    @NotBlank
    private String payMethod;
}
