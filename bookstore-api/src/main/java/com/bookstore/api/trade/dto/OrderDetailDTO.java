package com.bookstore.api.trade.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class OrderDetailDTO {

    private Long id;
    private Long userId;
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
    private List<OrderItemDTO> items;
}
