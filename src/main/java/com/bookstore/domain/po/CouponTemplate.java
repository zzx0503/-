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
@TableName("coupon_template")
public class CouponTemplate extends BaseEntity {

    private String name;
    private String type;
    private BigDecimal threshold;

    @TableField("discount_value")
    private BigDecimal discountValue;

    private Integer totalCount;
    private Integer claimedCount;
    private LocalDateTime validFrom;
    private LocalDateTime validTo;
    private String status;
    private String description;
}
