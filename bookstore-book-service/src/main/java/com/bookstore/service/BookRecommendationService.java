package com.bookstore.service;

import com.bookstore.domain.vo.book.BookListVO;

import java.util.List;

public interface BookRecommendationService {

    List<BookListVO> recommendForUser(Long userId, Integer limit);

    List<BookListVO> similarBooks(Long bookId, Integer limit);
}
