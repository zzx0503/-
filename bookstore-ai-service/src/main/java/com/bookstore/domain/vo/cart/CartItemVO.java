package com.bookstore.domain.vo.cart;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class CartItemVO {

    private Long id;
    private Long bookId;
    private String bookTitle;
    private String bookCover;
    private BigDecimal bookPrice;
    private Integer bookStock;
    private Integer quantity;
    private Integer selected;
    private LocalDateTime createTime;
}
