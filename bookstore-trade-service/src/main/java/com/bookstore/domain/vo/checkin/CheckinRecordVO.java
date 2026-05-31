package com.bookstore.domain.vo.checkin;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class CheckinRecordVO {

    private LocalDate checkinDate;
    private int consecutiveDays;
    private BigDecimal rewardAmount;
}
