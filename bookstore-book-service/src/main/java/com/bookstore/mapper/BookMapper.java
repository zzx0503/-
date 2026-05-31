package com.bookstore.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bookstore.domain.po.Book;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface BookMapper extends BaseMapper<Book> {

    @Select("SELECT * FROM book WHERE isbn = #{isbn} LIMIT 1")
    Book selectByIsbn(@Param("isbn") String isbn);
}
