package com.bookstore.domain.po;

import com.baomidou.mybatisplus.annotation.TableName;
import com.bookstore.domain.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("checkin_reward_rule")
public class CheckinRewardRule extends BaseEntity {

    private Integer consecutiveDays;
    private BigDecimal rewardAmount;
    private String bonusType;
    private String description;
}
