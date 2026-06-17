package com.bookstore.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bookstore.config.AiAgentProperties;
import com.bookstore.config.AiProperties;
import com.bookstore.domain.dto.ai.ChatRequestDTO;
import com.bookstore.domain.dto.ai.RenameSessionDTO;
import com.bookstore.domain.po.AiChatMessage;
import com.bookstore.domain.po.AiChatSession;
import com.bookstore.domain.po.Book;
import com.bookstore.domain.vo.ai.ChatMessageVO;
import com.bookstore.domain.vo.ai.ChatReplyVO;
import com.bookstore.domain.vo.ai.ChatSessionVO;
import com.bookstore.domain.vo.book.BookListVO;
import com.bookstore.exception.BusinessException;
import com.bookstore.mapper.AiChatMessageMapper;
import com.bookstore.mapper.AiChatSessionMapper;
import com.bookstore.mapper.BookMapper;
import com.bookstore.response.ResultCode;
import com.bookstore.service.AiChatService;
import com.bookstore.service.ai.AiAgentClient;
import com.bookstore.service.ai.AiClient;
import com.bookstore.service.ai.AiClient.ChatMsg;
import com.bookstore.utils.OssUrlBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class AiChatServiceImpl implements AiChatService {

    private final AiChatSessionMapper sessionMapper;
    private final AiChatMessageMapper messageMapper;
    private final BookMapper bookMapper;
    private final AiClient aiClient;
    private final AiAgentClient aiAgentClient;
    private final AiProperties aiProps;
    private final AiAgentProperties agentProps;
    private final OssUrlBuilder ossUrlBuilder;

    private static final int CANDIDATES_LIMIT = 6;
    private static final int TITLE_PREVIEW_LIMIT = 30;
    private static final int LAST_MSG_PREVIEW = 200;

    @Override
    @Transactional
    public ChatReplyVO chat(Long userId, ChatRequestDTO dto) {
        AiChatSession session = ensureSession(userId, dto.getSessionId(), dto.getMessage());

        AiChatMessage userMsg = persistMessage(session.getId(), userId, "user", dto.getMessage(), null);

        List<Book> candidates = retrieveBookCandidates(dto.getMessage(), CANDIDATES_LIMIT);

        String reply;
        List<Book> referenced;

        if (agentProps.isEnabled()) {
            try {
                List<Map<String, Object>> candidateMaps = candidates.stream()
                    .map(this::toCandidateMap).toList();
                List<Map<String, Object>> history = loadHistory(
                    session.getId(), aiProps.getHistoryLimit(), userMsg.getId()
                ).stream().map(m -> Map.<String, Object>of("role", m.getRole(), "content", m.getContent()))
                 .toList();

                var agentResp = aiAgentClient.chat(
                    userId, dto.getMessage(), session.getId(), history, candidateMaps);
                reply = agentResp.reply();
                referenced = matchReferencedBooks(reply, candidates);
            } catch (BusinessException ex) {
                if (agentProps.isFallbackOnFailure()) {
                    log.warn("Agent failed, fallback to direct LLM: {}", ex.getMessage());
                    reply = directChatReply(session, userMsg, candidates, dto.getMessage());
                    referenced = matchReferencedBooks(reply, candidates);
                } else {
                    throw ex;
                }
            }
        } else {
            reply = directChatReply(session, userMsg, candidates, dto.getMessage());
            referenced = matchReferencedBooks(reply, candidates);
        }
        String refIds = referenced.isEmpty()
            ? null
            : referenced.stream().map(b -> b.getId().toString()).collect(Collectors.joining(","));

        AiChatMessage assistantMsg = persistMessage(session.getId(), userId, "assistant", reply, refIds);

        session.setLastMessage(truncate(reply, LAST_MSG_PREVIEW));
        session.setLastActiveTime(LocalDateTime.now());
        sessionMapper.updateById(session);

        ChatReplyVO vo = new ChatReplyVO();
        vo.setSessionId(session.getId());
        vo.setUserMessageId(userMsg.getId());
        vo.setAssistantMessageId(assistantMsg.getId());
        vo.setReply(reply);
        vo.setReferencedBooks(referenced.stream().map(this::toListVO).collect(Collectors.toList()));
        vo.setCreateTime(assistantMsg.getCreateTime());
        return vo;
    }

    private String directChatReply(AiChatSession session, AiChatMessage userMsg,
                                    List<Book> candidates, String userMessage) {
        String ragContext = buildContextSnippet(candidates);
        List<ChatMsg> messages = new ArrayList<>();
        String systemPrompt = aiProps.getSystemPrompt();
        if (!ragContext.isEmpty()) {
            systemPrompt = systemPrompt + "\n\n图书目录参考(可在回复中按需引用):\n" + ragContext;
        }
        messages.add(ChatMsg.system(systemPrompt));
        for (AiChatMessage m : loadHistory(session.getId(), aiProps.getHistoryLimit(), userMsg.getId())) {
            messages.add(new ChatMsg(m.getRole(), m.getContent()));
        }
        messages.add(ChatMsg.user(userMessage));
        return aiClient.chatCompletion(messages);
    }

    private Map<String, Object> toCandidateMap(Book b) {
        Map<String, Object> m = new java.util.LinkedHashMap<>();
        m.put("id", b.getId());
        m.put("title", b.getTitle());
        m.put("author", b.getAuthor());
        m.put("price", b.getPrice() == null ? null : b.getPrice().toPlainString());
        m.put("description", b.getDescription());
        return m;
    }

    @Override
    public List<ChatSessionVO> listSessions(Long userId) {
        List<AiChatSession> list = sessionMapper.selectList(
            new LambdaQueryWrapper<AiChatSession>()
                .eq(AiChatSession::getUserId, userId)
                .eq(AiChatSession::getDeleted, 0)
                .orderByDesc(AiChatSession::getLastActiveTime)
                .orderByDesc(AiChatSession::getId)
        );
        return list.stream().map(this::toSessionVO).collect(Collectors.toList());
    }

    @Override
    public ChatSessionVO createSession(Long userId, String title) {
        AiChatSession s = new AiChatSession();
        s.setUserId(userId);
        s.setTitle(hasText(title) ? truncate(title, 60) : "新的对话");
        s.setLastActiveTime(LocalDateTime.now());
        sessionMapper.insert(s);
        return toSessionVO(s);
    }

    @Override
    public void renameSession(Long userId, Long sessionId, RenameSessionDTO dto) {
        AiChatSession s = requireSession(userId, sessionId);
        s.setTitle(truncate(dto.getTitle(), 60));
        sessionMapper.updateById(s);
    }

    @Override
    public void deleteSession(Long userId, Long sessionId) {
        AiChatSession s = requireSession(userId, sessionId);
        sessionMapper.deleteById(s.getId());
        messageMapper.delete(
            new LambdaQueryWrapper<AiChatMessage>().eq(AiChatMessage::getSessionId, sessionId)
        );
    }

    @Override
    public List<ChatMessageVO> listMessages(Long userId, Long sessionId) {
        requireSession(userId, sessionId);
        List<AiChatMessage> messages = messageMapper.selectList(
            new LambdaQueryWrapper<AiChatMessage>()
                .eq(AiChatMessage::getSessionId, sessionId)
                .eq(AiChatMessage::getDeleted, 0)
                .orderByAsc(AiChatMessage::getId)
        );
        Set<Long> bookIds = new LinkedHashSet<>();
        for (AiChatMessage m : messages) {
            for (Long id : parseRefIds(m.getReferencedBookIds())) {
                bookIds.add(id);
            }
        }
        Map<Long, BookListVO> bookMap = loadBookMap(bookIds);

        List<ChatMessageVO> result = new ArrayList<>(messages.size());
        for (AiChatMessage m : messages) {
            ChatMessageVO vo = new ChatMessageVO();
            vo.setId(m.getId());
            vo.setSessionId(m.getSessionId());
            vo.setRole(m.getRole());
            vo.setContent(m.getContent());
            vo.setCreateTime(m.getCreateTime());
            List<BookListVO> refs = new ArrayList<>();
            for (Long id : parseRefIds(m.getReferencedBookIds())) {
                BookListVO b = bookMap.get(id);
                if (b != null) refs.add(b);
            }
            vo.setReferencedBooks(refs);
            result.add(vo);
        }
        return result;
    }

    private AiChatSession ensureSession(Long userId, Long sessionId, String firstMessage) {
        if (sessionId != null) {
            return requireSession(userId, sessionId);
        }
        AiChatSession s = new AiChatSession();
        s.setUserId(userId);
        s.setTitle(deriveTitle(firstMessage));
        s.setLastActiveTime(LocalDateTime.now());
        sessionMapper.insert(s);
        return s;
    }

    private AiChatSession requireSession(Long userId, Long sessionId) {
        AiChatSession s = sessionMapper.selectById(sessionId);
        if (s == null || s.getDeleted() == 1 || !s.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.AI_SESSION_NOT_FOUND);
        }
        return s;
    }

    private AiChatMessage persistMessage(Long sessionId, Long userId, String role, String content, String refIds) {
        AiChatMessage m = new AiChatMessage();
        m.setSessionId(sessionId);
        m.setUserId(userId);
        m.setRole(role);
        m.setContent(content);
        m.setReferencedBookIds(refIds);
        messageMapper.insert(m);
        return m;
    }

    private List<AiChatMessage> loadHistory(Long sessionId, Integer historyLimit, Long excludeId) {
        int limit = historyLimit == null || historyLimit < 1 ? 10 : historyLimit;
        List<AiChatMessage> recent = messageMapper.selectList(
            new LambdaQueryWrapper<AiChatMessage>()
                .eq(AiChatMessage::getSessionId, sessionId)
                .eq(AiChatMessage::getDeleted, 0)
                .ne(excludeId != null, AiChatMessage::getId, excludeId)
                .orderByDesc(AiChatMessage::getId)
                .last("LIMIT " + limit)
        );
        Collections.reverse(recent);
        return recent;
    }

    private List<Book> retrieveBookCandidates(String message, int limit) {
        Set<Long> ids = new LinkedHashSet<>();
        List<Book> result = new ArrayList<>();
        if (hasText(message)) {
            String trimmed = message.trim();
            String kw = trimmed.length() > 12 ? trimmed.substring(0, 12) : trimmed;
            List<Book> matches = bookMapper.selectList(
                new LambdaQueryWrapper<Book>()
                    .eq(Book::getStatus, 1)
                    .eq(Book::getDeleted, 0)
                    .and(w -> w.like(Book::getTitle, kw)
                        .or().like(Book::getAuthor, kw)
                        .or().like(Book::getDescription, kw))
                    .orderByDesc(Book::getSalesCount)
                    .last("LIMIT " + limit)
            );
            for (Book b : matches) {
                if (ids.add(b.getId())) result.add(b);
            }
        }
        if (result.size() < limit) {
            int remaining = limit - result.size();
            List<Book> hot = bookMapper.selectList(
                new LambdaQueryWrapper<Book>()
                    .eq(Book::getStatus, 1)
                    .eq(Book::getDeleted, 0)
                    .notIn(!ids.isEmpty(), Book::getId, ids)
                    .orderByDesc(Book::getSalesCount)
                    .last("LIMIT " + remaining)
            );
            for (Book b : hot) {
                if (ids.add(b.getId())) result.add(b);
            }
        }
        return result;
    }

    private String buildContextSnippet(List<Book> books) {
        if (books == null || books.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        int idx = 1;
        for (Book b : books) {
            sb.append(idx++).append(". 《").append(safe(b.getTitle())).append("》");
            if (hasText(b.getAuthor())) sb.append(" 作者:").append(b.getAuthor());
            if (b.getPrice() != null) sb.append(" 价格:¥").append(b.getPrice().toPlainString());
            String desc = safe(b.getDescription());
            if (!desc.isEmpty()) {
                sb.append(" 简介:").append(truncate(desc.replaceAll("\\s+", " "), 80));
            }
            sb.append('\n');
        }
        return sb.toString();
    }

    private List<Book> matchReferencedBooks(String reply, List<Book> candidates) {
        if (!hasText(reply) || candidates == null || candidates.isEmpty()) return Collections.emptyList();
        List<Book> matched = new ArrayList<>();
        for (Book b : candidates) {
            if (!hasText(b.getTitle())) continue;
            if (reply.contains("《" + b.getTitle() + "》") || reply.contains(b.getTitle())) {
                matched.add(b);
            }
        }
        return matched;
    }

    private List<Long> parseRefIds(String s) {
        if (!hasText(s)) return Collections.emptyList();
        List<Long> ids = new ArrayList<>();
        for (String part : s.split(",")) {
            String t = part.trim();
            if (t.isEmpty()) continue;
            try {
                ids.add(Long.parseLong(t));
            } catch (NumberFormatException ignore) {
            }
        }
        return ids;
    }

    private Map<Long, BookListVO> loadBookMap(Set<Long> ids) {
        if (ids.isEmpty()) return Collections.emptyMap();
        List<Book> books = bookMapper.selectList(
            new LambdaQueryWrapper<Book>()
                .in(Book::getId, ids)
                .eq(Book::getDeleted, 0)
        );
        return books.stream().collect(Collectors.toMap(Book::getId, this::toListVO));
    }

    private BookListVO toListVO(Book b) {
        BookListVO vo = new BookListVO();
        vo.setId(b.getId());
        vo.setTitle(b.getTitle());
        vo.setSubtitle(b.getSubtitle());
        vo.setAuthor(b.getAuthor());
        vo.setCoverKey(ossUrlBuilder.toFullUrl(b.getCoverKey()));
        vo.setPrice(b.getPrice());
        vo.setOriginalPrice(b.getOriginalPrice());
        vo.setSalesCount(b.getSalesCount());
        vo.setRating(b.getRating());
        return vo;
    }

    private ChatSessionVO toSessionVO(AiChatSession s) {
        ChatSessionVO vo = new ChatSessionVO();
        vo.setId(s.getId());
        vo.setTitle(s.getTitle());
        vo.setLastMessage(s.getLastMessage());
        vo.setLastActiveTime(s.getLastActiveTime());
        vo.setCreateTime(s.getCreateTime());
        return vo;
    }

    private String deriveTitle(String firstMessage) {
        if (!hasText(firstMessage)) return "新的对话";
        String t = firstMessage.trim().replaceAll("\\s+", " ");
        return truncate(t, TITLE_PREVIEW_LIMIT);
    }

    private static boolean hasText(String s) {
        return s != null && !s.trim().isEmpty();
    }

    private static String safe(String s) {
        return s == null ? "" : s;
    }

    private static String truncate(String s, int max) {
        if (s == null) return null;
        return s.length() <= max ? s : s.substring(0, max);
    }
}
