package com.bookstore.controller;

import com.bookstore.domain.dto.book.BookQueryDTO;
import com.bookstore.api.book.dto.BookDetailDTO;
import com.bookstore.api.book.dto.BookListDTO;
import com.bookstore.response.PageResult;
import com.bookstore.response.Result;
import com.bookstore.service.BookRecommendationService;
import com.bookstore.service.BookService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 图书控制器
 * 提供图书浏览、搜索、推荐等功能
 */
@Tag(name = "图书", description = "图书浏览与搜索")
@RestController
@RequestMapping("/api/book")
@RequiredArgsConstructor
public class BookController {

    private final BookService bookService;
    private final BookRecommendationService bookRecommendationService;

    /**
     * 分页查询图书列表
     * 支持按分类、价格范围等条件筛选
     * @param query 查询条件（分类ID、价格范围、排序等）
     * @return 分页图书列表
     */
    @GetMapping
    public Result<PageResult<BookListDTO>> list(BookQueryDTO query) {
        return Result.success(bookService.list(query));
    }

    /**
     * 获取图书详情
     * @param id 图书ID
     * @return 图书详细信息
     */
    @GetMapping("/{id}")
    public Result<BookDetailDTO> detail(@PathVariable Long id) {
        return Result.success(bookService.detail(id));
    }

    /**
     * 获取热销图书榜单
     * @param limit 返回数量，默认10本
     * @return 热销图书列表
     */
    @GetMapping("/hot")
    public Result<List<BookListDTO>> hot(@RequestParam(defaultValue = "10") Integer limit) {
        return Result.success(bookService.hot(limit));
    }

    /**
     * 获取最新上架图书
     * @param limit 返回数量，默认10本
     * @return 新书列表
     */
    @GetMapping("/new")
    public Result<List<BookListDTO>> newest(@RequestParam(defaultValue = "10") Integer limit) {
        return Result.success(bookService.newest(limit));
    }

    /**
     * 传统搜索图书（保留兜底）
     * @param keyword 搜索关键词
     * @param page 页码
     * @param size 每页数量
     * @return 搜索结果分页列表
     */
    @GetMapping("/search")
    public Result<PageResult<BookListDTO>> search(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        return Result.success(bookService.search(keyword, page, size));
    }

    /**
     * 获取相似图书推荐
     * 基于图书内容向量相似度计算
     * @param id 基准图书ID
     * @param limit 返回数量，默认6本
     * @return 相似图书列表
     */
    @Operation(summary = "相似图书")
    @GetMapping("/{id}/similar")
    public Result<List<BookListDTO>> similar(@PathVariable Long id, @RequestParam(defaultValue = "6") Integer limit) {
        return Result.success(bookRecommendationService.similarBooks(id, limit));
    }
}
