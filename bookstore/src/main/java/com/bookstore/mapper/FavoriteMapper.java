package com.bookstore.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bookstore.domain.po.Favorite;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

public interface FavoriteMapper extends BaseMapper<Favorite> {

    @Update("UPDATE favorite SET deleted = 0, update_time = NOW() WHERE user_id = #{userId} AND book_id = #{bookId} AND deleted = 1")
    int restoreDeleted(@Param("userId") Long userId, @Param("bookId") Long bookId);
}
