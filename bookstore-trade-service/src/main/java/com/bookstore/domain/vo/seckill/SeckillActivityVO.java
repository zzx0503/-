package com.bookstore.domain.vo.seckill;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class SeckillActivityVO {

    private Long id;
    private Long bookId;
    private String bookTitle;
    private String bookCover;
    private String bookAuthor;
    private BigDecimal seckillPrice;
    private BigDecimal originalPrice;
    private Integer totalStock;
    private Integer soldCount;
    private Integer remainingStock;
    private Integer perUserLimit;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String status;
    private String title;
    private String userStatus;
    private LocalDateTime createTime;
}
