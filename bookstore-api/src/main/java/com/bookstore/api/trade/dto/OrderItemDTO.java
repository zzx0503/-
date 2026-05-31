package com.bookstore.api.trade.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class OrderItemDTO {

    private Long id;
    private Long bookId;
    private String bookTitle;
    private String bookCover;
    private BigDecimal unitPrice;
    private Integer quantity;
    private BigDecimal subtotal;
}
