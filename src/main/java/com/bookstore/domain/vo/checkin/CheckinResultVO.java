package com.bookstore.domain.vo.checkin;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CheckinResultVO {

    private boolean success;
    private int consecutiveDays;
    private BigDecimal rewardAmount;
    private LocalDate checkinDate;
}
