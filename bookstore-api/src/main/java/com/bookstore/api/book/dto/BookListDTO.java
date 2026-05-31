package com.bookstore.api.book.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class BookListDTO {

    private Long id;
    private String title;
    private String subtitle;
    private String author;
    private String coverKey;
    private BigDecimal price;
    private BigDecimal originalPrice;
    private Integer salesCount;
    private BigDecimal rating;
    private Long categoryId;
    private String categoryName;
}
