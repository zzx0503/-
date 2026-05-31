package com.bookstore.controller;

import com.bookstore.domain.vo.book.BookDetailVO;
import com.bookstore.response.Result;
import com.bookstore.service.BookService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/internal/book")
@RequiredArgsConstructor
public class InternalBookController {

    private final BookService bookService;

    @GetMapping("/{id}")
    public Result<BookDetailVO> getBook(@PathVariable Long id) {
        return Result.success(bookService.detail(id));
    }

    @PostMapping("/{id}/deduct-stock")
    public Result<Void> deductStock(@PathVariable Long id,
                                    @RequestParam Integer quantity) {
        boolean ok = bookService.deductStock(id, quantity);
        if (!ok) {
            return Result.fail(com.bookstore.response.ResultCode.STOCK_INSUFFICIENT);
        }
        return Result.success();
    }

    @PostMapping("/{id}/restore-stock")
    public Result<Void> restoreStock(@PathVariable Long id,
                                     @RequestParam Integer quantity) {
        bookService.restoreStock(id, quantity);
        return Result.success();
    }
}
