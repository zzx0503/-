package com.bookstore.domain.vo.book;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
public class BookDetailVO {

    private Long id;
    private String isbn;
    private String title;
    private String subtitle;
    private String author;
    private String translator;
    private String publisher;
    private Long categoryId;
    private String categoryName;
    private String coverKey;
    private BigDecimal price;
    private BigDecimal originalPrice;
    private Integer stock;
    private Integer salesCount;
    private BigDecimal rating;
    private String description;
    private LocalDate publishDate;
    private Integer status;
    private Integer deleted;
    private Boolean isFavorited;
    private List<BookListVO> relatedBooks;
}
