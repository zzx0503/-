package com.bookstore.domain.dto.book;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class BookQueryDTO {

    private Long categoryId;
    private String keyword;
    private BigDecimal minPrice;
    private BigDecimal maxPrice;
    private String sortField;
    private String sortOrder;
    private Integer page = 1;
    private Integer size = 10;
}
