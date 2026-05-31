package com.bookstore.service;

import com.bookstore.api.book.dto.BookListDTO;

import java.util.List;

public interface BookRecommendationService {

    List<BookListDTO> recommendForUser(Long userId, Integer limit);

    List<BookListDTO> similarBooks(Long bookId, Integer limit);
}
