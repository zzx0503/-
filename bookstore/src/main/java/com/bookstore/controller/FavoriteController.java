package com.bookstore.controller;

import com.bookstore.anno.LoginRequired;
import com.bookstore.context.UserContext;
import com.bookstore.domain.vo.favorite.FavoriteVO;
import com.bookstore.response.PageResult;
import com.bookstore.response.Result;
import com.bookstore.service.FavoriteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "收藏", description = "图书收藏")
@RestController
@RequestMapping("/api/favorite")
@RequiredArgsConstructor
@LoginRequired
public class FavoriteController {

    private final FavoriteService favoriteService;

    @Operation(summary = "添加收藏")
    @PostMapping("/{bookId}")
    public Result<Void> add(@PathVariable Long bookId) {
        favoriteService.addFavorite(UserContext.requireUserId(), bookId);
        return Result.success();
    }

    @Operation(summary = "取消收藏")
    @DeleteMapping("/{bookId}")
    public Result<Void> remove(@PathVariable Long bookId) {
        favoriteService.removeFavorite(UserContext.requireUserId(), bookId);
        return Result.success();
    }

    @Operation(summary = "查询收藏列表")
    @GetMapping
    public Result<PageResult<FavoriteVO>> list(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        return Result.success(favoriteService.listFavorites(UserContext.requireUserId(), page, size));
    }
}
