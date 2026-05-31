package com.bookstore.service;

import com.bookstore.domain.dto.book.BookFormDTO;
import com.bookstore.domain.dto.book.BookQueryDTO;
import com.bookstore.api.book.dto.BookDetailDTO;
import com.bookstore.api.book.dto.BookListDTO;
import com.bookstore.domain.vo.book.BookVO;
import com.bookstore.response.PageResult;

import java.util.List;

public interface BookService {

    PageResult<BookListDTO> list(BookQueryDTO query);

    BookDetailDTO detail(Long id);

    List<BookListDTO> hot(Integer limit);

    List<BookListDTO> newest(Integer limit);

    PageResult<BookListDTO> search(String keyword, Integer page, Integer size);

    BookVO create(BookFormDTO dto);

    BookVO update(Long id, BookFormDTO dto);

    void delete(Long id);

    void toggleStatus(Long id);

    void adjustStock(Long id, Integer delta);

    boolean deductStock(Long id, Integer quantity);

    void restoreStock(Long id, Integer quantity);
}
