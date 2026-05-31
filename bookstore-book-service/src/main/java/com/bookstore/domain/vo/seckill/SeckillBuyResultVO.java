package com.bookstore.domain.vo.seckill;

import lombok.Data;

@Data
public class SeckillBuyResultVO {

    private String orderNo;
    private Long seckillOrderId;
    private Long expireSeconds;
}
