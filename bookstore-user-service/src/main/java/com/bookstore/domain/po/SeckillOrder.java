package com.bookstore.domain.po;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.bookstore.domain.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("seckill_order")
public class SeckillOrder extends BaseEntity {

    private String orderNo;
    private Long userId;
    private Long activityId;
    private Long bookId;
    private Integer quantity;
    private BigDecimal seckillPrice;
    private BigDecimal totalAmount;
    private String status;
    private LocalDateTime payTime;
    private LocalDateTime expireTime;

    @TableField("address_snapshot")
    private String addressSnapshot;
}
