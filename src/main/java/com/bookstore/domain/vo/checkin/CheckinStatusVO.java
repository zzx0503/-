package com.bookstore.domain.vo.checkin;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
public class CheckinStatusVO {

    private boolean checkedInToday;
    private int currentStreak;
    private BigDecimal todayReward;
    private List<LocalDate> monthCheckedDates;
}
