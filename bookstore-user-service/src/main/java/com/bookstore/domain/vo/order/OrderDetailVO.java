package com.bookstore.domain.vo.order;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class OrderDetailVO {

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
    private String addressSnapshot;
    private LocalDateTime shipTime;
    private LocalDateTime completeTime;
    private LocalDateTime createTime;
    private List<OrderItemVO> items;
}
