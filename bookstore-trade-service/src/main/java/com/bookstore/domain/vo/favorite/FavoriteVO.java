package com.bookstore.domain.vo.favorite;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class FavoriteVO {

    private Long id;
    private Long bookId;
    private String bookTitle;
    private String bookCover;
    private String bookAuthor;
    private BigDecimal bookPrice;
    private Long bookCategoryId;
    private String bookCategoryName;
    private LocalDateTime createTime;
}
