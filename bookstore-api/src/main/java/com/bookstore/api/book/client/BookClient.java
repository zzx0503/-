package com.bookstore.api.book.client;

import com.bookstore.api.book.dto.BookDetailDTO;
import com.bookstore.api.book.fallback.BookClientFallbackFactory;
import com.bookstore.response.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "book-service", fallbackFactory = BookClientFallbackFactory.class)
public interface BookClient {

    @GetMapping("/api/internal/book/{id}")
    Result<BookDetailDTO> getBook(@PathVariable("id") Long id);

    @PostMapping("/api/internal/book/{id}/deduct-stock")
    Result<Void> deductStock(@PathVariable("id") Long id, @RequestParam("quantity") Integer quantity);

    @PostMapping("/api/internal/book/{id}/restore-stock")
    Result<Void> restoreStock(@PathVariable("id") Long id, @RequestParam("quantity") Integer quantity);
}
