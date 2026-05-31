package com.bookstore.domain.vo.book;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class BookVO {

    private Long id;
    private String isbn;
    private String title;
    private String subtitle;
    private String author;
    private String translator;
    private String publisher;
    private Long categoryId;
    private String coverKey;
    private BigDecimal price;
    private BigDecimal originalPrice;
    private Integer stock;
    private Integer salesCount;
    private BigDecimal rating;
    private String description;
    private LocalDate publishDate;
    private Integer status;
    private LocalDateTime createTime;
}
