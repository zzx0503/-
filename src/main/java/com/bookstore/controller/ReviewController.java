package com.bookstore.controller;

import com.bookstore.anno.LoginRequired;
import com.bookstore.context.UserContext;
import com.bookstore.domain.dto.review.ReviewFormDTO;
import com.bookstore.domain.vo.review.ReviewVO;
import com.bookstore.response.PageResult;
import com.bookstore.response.Result;
import com.bookstore.service.ReviewService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "评价", description = "图书评价")
@RestController
@RequestMapping("/api/review")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    @PostMapping
    @LoginRequired
    public Result<ReviewVO> create(@Valid @RequestBody ReviewFormDTO dto) {
        return Result.success(reviewService.createReview(UserContext.requireUserId(), dto));
    }

    @GetMapping("/book/{bookId}")
    public Result<PageResult<ReviewVO>> listByBook(@PathVariable Long bookId,
                                                   @RequestParam(defaultValue = "1") Integer page,
                                                   @RequestParam(defaultValue = "10") Integer size) {
        return Result.success(reviewService.listByBook(bookId, page, size));
    }

    @DeleteMapping("/{id}")
    @LoginRequired
    public Result<Void> delete(@PathVariable Long id) {
        reviewService.deleteReview(UserContext.requireUserId(), id);
        return Result.success();
    }
}
