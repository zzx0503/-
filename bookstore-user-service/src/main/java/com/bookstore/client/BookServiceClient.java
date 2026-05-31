package com.bookstore.client;

import com.bookstore.domain.vo.book.BookDetailVO;
import com.bookstore.response.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "book-service")
public interface BookServiceClient {

    @GetMapping("/api/internal/book/{id}")
    Result<BookDetailVO> getBook(@PathVariable("id") Long id);
}
