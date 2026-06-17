package com.bookstore.controller;

import com.bookstore.config.AiAgentProperties;
import com.bookstore.response.Result;
import com.bookstore.service.InternalToolService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@RestController
@RequestMapping("/internal/agent-tools")
@RequiredArgsConstructor
public class InternalToolController {

    private final InternalToolService toolService;
    private final AiAgentProperties agentProps;

    @PostMapping("/search-books")
    public Result<?> searchBooks(@RequestHeader("X-API-Key") String apiKey,
                                 @RequestBody Map<String, Object> req) {
        validateApiKey(apiKey);
        String keyword = (String) req.get("keyword");
        int limit = toInt(req.get("limit"), 10);
        return Result.success(toolService.searchBooks(keyword, limit));
    }

    @PostMapping("/book-detail")
    public Result<?> bookDetail(@RequestHeader("X-API-Key") String apiKey,
                                @RequestBody Map<String, Object> req) {
        validateApiKey(apiKey);
        Long bookId = Long.valueOf(req.get("book_id").toString());
        return Result.success(toolService.getBookDetail(bookId));
    }

    @PostMapping("/user-profile")
    public Result<?> userProfile(@RequestHeader("X-API-Key") String apiKey,
                                 @RequestBody Map<String, Object> req) {
        validateApiKey(apiKey);
        Long userId = Long.valueOf(req.get("user_id").toString());
        return Result.success(toolService.getUserProfile(userId));
    }

    @PostMapping("/add-to-cart")
    public Result<?> addToCart(@RequestHeader("X-API-Key") String apiKey,
                               @RequestBody Map<String, Object> req) {
        validateApiKey(apiKey);
        Long userId = Long.valueOf(req.get("user_id").toString());
        Long bookId = Long.valueOf(req.get("book_id").toString());
        int qty = toInt(req.get("quantity"), 1);
        return Result.success(toolService.addToCart(userId, bookId, qty));
    }

    private void validateApiKey(String apiKey) {
        if (!agentProps.isEnabled() || !apiKey.equals(agentProps.getApiKey())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Invalid API key");
        }
    }

    private static int toInt(Object o, int def) {
        return o instanceof Number n ? n.intValue() : def;
    }
}
