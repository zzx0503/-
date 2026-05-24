package com.bookstore.domain.vo.seckill;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class SeckillOrderVO {

    private Long id;
    private String orderNo;
    private Long activityId;
    private Long bookId;
    private String bookTitle;
    private String bookCover;
    private Integer quantity;
    private BigDecimal seckillPrice;
    private BigDecimal totalAmount;
    private String status;
    private LocalDateTime payTime;
    private LocalDateTime expireTime;
    private LocalDateTime createTime;
}
