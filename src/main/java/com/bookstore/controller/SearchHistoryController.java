package com.bookstore.controller;

import com.bookstore.anno.LoginRequired;
import com.bookstore.context.UserContext;
import com.bookstore.domain.po.SearchHistory;
import com.bookstore.response.Result;
import com.bookstore.service.SearchHistoryService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "搜索历史", description = "搜索历史管理")
@RestController
@RequestMapping("/api/search-history")
@RequiredArgsConstructor
@LoginRequired
public class SearchHistoryController {

    private final SearchHistoryService searchHistoryService;

    @GetMapping
    public Result<List<SearchHistory>> list(
            @RequestParam(defaultValue = "10") Integer limit) {
        return Result.success(searchHistoryService.listRecent(UserContext.requireUserId(), limit));
    }

    @DeleteMapping
    public Result<Void> clear() {
        searchHistoryService.clear(UserContext.requireUserId());
        return Result.success();
    }
}
