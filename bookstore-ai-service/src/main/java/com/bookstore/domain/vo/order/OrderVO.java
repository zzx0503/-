package com.bookstore.domain.vo.order;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class OrderVO {

    private Long id;
    private String orderNo;
    private BigDecimal totalAmount;
    private BigDecimal payAmount;
    private BigDecimal discountAmount;
    private Long couponId;
    private String payMethod;
    private LocalDateTime payTime;
    private String status;
    private String remark;
    private LocalDateTime shipTime;
    private LocalDateTime completeTime;
    private LocalDateTime createTime;
}
