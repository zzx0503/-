package com.bookstore.controller;

import com.bookstore.context.UserContext;
import com.bookstore.domain.dto.book.BookQueryDTO;
import com.bookstore.domain.vo.book.BookDetailVO;
import com.bookstore.domain.vo.book.BookListVO;
import com.bookstore.response.PageResult;
import com.bookstore.response.Result;
import com.bookstore.service.AiBookAgentService;
import com.bookstore.service.BookRecommendationService;
import com.bookstore.service.BookService;
import com.bookstore.service.FavoriteService;
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
    private final FavoriteService favoriteService;
    private final BookRecommendationService bookRecommendationService;
    private final AiBookAgentService aiBookAgentService;

    /**
     * 分页查询图书列表
     * 支持按分类、价格范围等条件筛选
     * @param query 查询条件（分类ID、价格范围、排序等）
     * @return 分页图书列表
     */
    @Operation(summary = "分页查询图书列表")
    @GetMapping
    public Result<PageResult<BookListVO>> list(BookQueryDTO query) {
        return Result.success(bookService.list(query));
    }

    /**
     * 获取图书详情
     * 如果用户已登录，同时返回收藏状态
     * @param id 图书ID
     * @return 图书详细信息
     */
    @Operation(summary = "获取图书详情")
    @GetMapping("/{id}")
    public Result<BookDetailVO> detail(@PathVariable Long id) {
        BookDetailVO vo = bookService.detail(id);
        com.bookstore.context.CurrentUser cu = UserContext.get();
        if (cu != null) {
            vo.setIsFavorited(favoriteService.isFavorited(cu.getUserId(), id));
        }
        return Result.success(vo);
    }

    /**
     * 获取热销图书榜单
     * @param limit 返回数量，默认10本
     * @return 热销图书列表
     */
    @Operation(summary = "热销图书榜单")
    @GetMapping("/hot")
    public Result<List<BookListVO>> hot(@RequestParam(defaultValue = "10") Integer limit) {
        return Result.success(bookService.hot(limit));
    }

    /**
     * 获取最新上架图书
     * @param limit 返回数量，默认10本
     * @return 新书列表
     */
    @Operation(summary = "最新上架图书")
    @GetMapping("/new")
    public Result<List<BookListVO>> newest(@RequestParam(defaultValue = "10") Integer limit) {
        return Result.success(bookService.newest(limit));
    }

    /**
     * AI 智能搜索图书
     * 使用自然语言理解用户意图，从候选图书中智能匹配
     * @param keyword 用户搜索词
     * @return 智能匹配的图书列表
     */
    @Operation(summary = "AI 智能搜索")
    @GetMapping("/ai-search")
    public Result<List<BookListVO>> aiSearch(@RequestParam String keyword) {
        com.bookstore.context.CurrentUser cu = UserContext.get();
        Long userId = cu != null ? cu.getUserId() : null;
        return Result.success(aiBookAgentService.aiSearch(keyword, userId));
    }

    /**
     * 传统搜索图书（保留兜底）
     * @param keyword 搜索关键词
     * @param page 页码
     * @param size 每页数量
     * @return 搜索结果分页列表
     */
    @Operation(summary = "搜索图书")
    @GetMapping("/search")
    public Result<PageResult<BookListVO>> search(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        return Result.success(bookService.search(keyword, page, size));
    }

    /**
     * AI 个性化推荐
     * 根据用户画像和候选图书，由大模型做个性化推荐
     * 未登录用户返回热销榜单
     * @param limit 推荐数量，默认10本
     * @return 推荐图书列表
     */
    @Operation(summary = "AI 个性化推荐")
    @GetMapping("/ai-recommend")
    public Result<List<BookListVO>> aiRecommend(@RequestParam(defaultValue = "10") Integer limit) {
        com.bookstore.context.CurrentUser cu = UserContext.get();
        if (cu == null) {
            return Result.success(bookService.hot(limit));
        }
        return Result.success(aiBookAgentService.aiRecommend(cu.getUserId(), limit));
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
    public Result<List<BookListVO>> similar(@PathVariable Long id, @RequestParam(defaultValue = "6") Integer limit) {
        return Result.success(bookRecommendationService.similarBooks(id, limit));
    }
}
