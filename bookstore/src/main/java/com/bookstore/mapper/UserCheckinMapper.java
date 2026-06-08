package com.bookstore.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bookstore.domain.po.UserCheckin;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.util.List;

public interface UserCheckinMapper extends BaseMapper<UserCheckin> {

    @Select("SELECT * FROM user_checkin WHERE user_id = #{userId} AND checkin_date = #{date} AND deleted = 0 LIMIT 1")
    UserCheckin selectByUserAndDate(@Param("userId") Long userId, @Param("date") LocalDate date);

    @Select("SELECT * FROM user_checkin WHERE user_id = #{userId} AND deleted = 0 ORDER BY checkin_date DESC LIMIT 1")
    UserCheckin selectLatestByUser(@Param("userId") Long userId);

    @Select("SELECT checkin_date FROM user_checkin WHERE user_id = #{userId} AND deleted = 0 AND checkin_date >= #{startDate} AND checkin_date <= #{endDate} ORDER BY checkin_date")
    List<LocalDate> selectDatesInRange(@Param("userId") Long userId, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);
}
