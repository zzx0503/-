package com.bookstore.domain.vo.seckill;

import lombok.Data;

@Data
public class SeckillQueueStatusVO {

    private String requestId;
    private String status;
    private String orderNo;
    private Long seckillOrderId;
    private Long expireSeconds;
    private String msg;
}
