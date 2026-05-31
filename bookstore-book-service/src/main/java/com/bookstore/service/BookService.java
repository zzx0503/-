package com.bookstore.service;

import com.bookstore.domain.dto.book.BookFormDTO;
import com.bookstore.domain.dto.book.BookQueryDTO;
import com.bookstore.domain.vo.book.BookDetailVO;
import com.bookstore.domain.vo.book.BookListVO;
import com.bookstore.domain.vo.book.BookVO;
import com.bookstore.response.PageResult;

import java.util.List;

public interface BookService {

    PageResult<BookListVO> list(BookQueryDTO query);

    BookDetailVO detail(Long id);

    List<BookListVO> hot(Integer limit);

    List<BookListVO> newest(Integer limit);

    PageResult<BookListVO> search(String keyword, Integer page, Integer size);

    BookVO create(BookFormDTO dto);

    BookVO update(Long id, BookFormDTO dto);

    void delete(Long id);

    void toggleStatus(Long id);

    void adjustStock(Long id, Integer delta);

    boolean deductStock(Long id, Integer quantity);

    void restoreStock(Long id, Integer quantity);
}
