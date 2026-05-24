package com.bookstore.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bookstore.domain.po.CartItem;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface CartItemMapper extends BaseMapper<CartItem> {

    @Select("SELECT * FROM cart_item WHERE user_id = #{userId} AND book_id = #{bookId} LIMIT 1")
    CartItem selectOneIgnoreDeleted(@Param("userId") Long userId, @Param("bookId") Long bookId);

    @Select("UPDATE cart_item SET quantity = #{quantity}, selected = #{selected}, deleted = 0, update_time = NOW() WHERE id = #{id}")
    void updateIgnoreDeleted(@Param("id") Long id, @Param("quantity") Integer quantity, @Param("selected") Integer selected);
}
