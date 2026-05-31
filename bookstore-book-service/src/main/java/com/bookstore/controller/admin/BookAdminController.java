package com.bookstore.controller.admin;

import com.bookstore.anno.AdminRequired;
import com.bookstore.domain.dto.book.BookFormDTO;
import com.bookstore.domain.vo.book.BookVO;
import com.bookstore.response.Result;
import com.bookstore.service.BookService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "图书管理", description = "后台图书管理")
@RestController
@RequestMapping("/api/admin/book")
@RequiredArgsConstructor
@AdminRequired
public class BookAdminController {

    private final BookService bookService;

    @PostMapping
    public Result<BookVO> create(@Valid @RequestBody BookFormDTO dto) {
        return Result.success(bookService.create(dto));
    }

    @PutMapping("/{id}")
    public Result<BookVO> update(@PathVariable Long id,
                                 @Valid @RequestBody BookFormDTO dto) {
        return Result.success(bookService.update(id, dto));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        bookService.delete(id);
        return Result.success();
    }

    @PutMapping("/{id}/status")
    public Result<Void> toggleStatus(@PathVariable Long id) {
        bookService.toggleStatus(id);
        return Result.success();
    }

    @PutMapping("/{id}/stock")
    public Result<Void> adjustStock(@PathVariable Long id,
                                    @RequestParam Integer delta) {
        bookService.adjustStock(id, delta);
        return Result.success();
    }
}
