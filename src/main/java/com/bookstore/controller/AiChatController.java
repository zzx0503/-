package com.bookstore.controller;

import com.bookstore.anno.LoginRequired;
import com.bookstore.anno.RateLimit;
import com.bookstore.context.UserContext;
import com.bookstore.domain.dto.ai.ChatRequestDTO;
import com.bookstore.domain.dto.ai.RenameSessionDTO;
import com.bookstore.domain.vo.ai.ChatMessageVO;
import com.bookstore.domain.vo.ai.ChatReplyVO;
import com.bookstore.domain.vo.ai.ChatSessionVO;
import com.bookstore.domain.vo.book.BookListVO;
import com.bookstore.response.Result;
import com.bookstore.service.AiChatService;
import com.bookstore.service.BookRecommendationService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@Tag(name = "AI助手", description = "AI 聊天与推荐")
@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AiChatController {

    private final AiChatService aiChatService;
    private final BookRecommendationService bookRecommendationService;

    @PostMapping("/chat")
    @LoginRequired
    @RateLimit(key = "ai:chat", qps = 2)
    public Result<ChatReplyVO> chat(@Valid @RequestBody ChatRequestDTO dto) {
        return Result.success(aiChatService.chat(UserContext.requireUserId(), dto));
    }

    @GetMapping("/sessions")
    @LoginRequired
    public Result<List<ChatSessionVO>> sessions() {
        return Result.success(aiChatService.listSessions(UserContext.requireUserId()));
    }

    @PostMapping("/sessions")
    @LoginRequired
    public Result<ChatSessionVO> createSession(@RequestBody(required = false) Map<String, String> body) {
        String title = body == null ? null : body.get("title");
        return Result.success(aiChatService.createSession(UserContext.requireUserId(), title));
    }

    @PutMapping("/sessions/{sessionId}")
    @LoginRequired
    public Result<Void> renameSession(@PathVariable Long sessionId, @Valid @RequestBody RenameSessionDTO dto) {
        aiChatService.renameSession(UserContext.requireUserId(), sessionId, dto);
        return Result.success();
    }

    @DeleteMapping("/sessions/{sessionId}")
    @LoginRequired
    public Result<Void> deleteSession(@PathVariable Long sessionId) {
        aiChatService.deleteSession(UserContext.requireUserId(), sessionId);
        return Result.success();
    }

    @GetMapping("/sessions/{sessionId}/messages")
    @LoginRequired
    public Result<List<ChatMessageVO>> messages(@PathVariable Long sessionId) {
        return Result.success(aiChatService.listMessages(UserContext.requireUserId(), sessionId));
    }

    @GetMapping("/recommend")
    public Result<List<BookListVO>> recommend(@RequestParam(defaultValue = "10") Integer limit) {
        Long userId = UserContext.get() == null ? null : UserContext.get().getUserId();
        return Result.success(bookRecommendationService.recommendForUser(userId, limit));
    }

    @GetMapping("/similar/{bookId}")
    public Result<List<BookListVO>> similar(@PathVariable Long bookId,
                                            @RequestParam(defaultValue = "6") Integer limit) {
        return Result.success(bookRecommendationService.similarBooks(bookId, limit));
    }
}
