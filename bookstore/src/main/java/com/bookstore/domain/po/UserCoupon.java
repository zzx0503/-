package com.bookstore.domain.po;

import com.baomidou.mybatisplus.annotation.TableName;
import com.bookstore.domain.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("user_coupon")
public class UserCoupon extends BaseEntity {

    private Long userId;
    private Long templateId;
    private String code;
    private String status;
    private String lockedOrderNo;
    private String usedOrderNo;
    private LocalDateTime usedAt;
    private LocalDateTime expireTime;
}
