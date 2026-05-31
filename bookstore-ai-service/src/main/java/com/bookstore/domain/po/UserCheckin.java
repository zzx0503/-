package com.bookstore.domain.po;

import com.baomidou.mybatisplus.annotation.TableName;
import com.bookstore.domain.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("user_checkin")
public class UserCheckin extends BaseEntity {

    private Long userId;
    private LocalDate checkinDate;
    private Integer consecutiveDays;
    private BigDecimal rewardAmount;
}
