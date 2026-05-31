package com.bookstore.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bookstore.config.AiProperties;
import com.bookstore.domain.dto.ai.ChatRequestDTO;
import com.bookstore.domain.dto.ai.RenameSessionDTO;
import com.bookstore.domain.po.AiChatMessage;
import com.bookstore.domain.po.AiChatSession;
import com.bookstore.domain.vo.ai.ChatMessageVO;
import com.bookstore.domain.vo.ai.ChatReplyVO;
import com.bookstore.domain.vo.ai.ChatSessionVO;
import com.bookstore.exception.BusinessException;
import com.bookstore.mapper.AiChatMessageMapper;
import com.bookstore.mapper.AiChatSessionMapper;
import com.bookstore.response.ResultCode;
import com.bookstore.service.AiChatService;
import com.bookstore.service.ai.AiClient;
import com.bookstore.service.ai.AiClient.ChatMsg;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AiChatServiceImpl implements AiChatService {

    private final AiChatSessionMapper sessionMapper;
    private final AiChatMessageMapper messageMapper;
    private final AiClient aiClient;
    private final AiProperties aiProps;

    private static final int TITLE_PREVIEW_LIMIT = 30;
    private static final int LAST_MSG_PREVIEW = 200;

    @Override
    @Transactional
    public ChatReplyVO chat(Long userId, ChatRequestDTO dto) {
        AiChatSession session = ensureSession(userId, dto.getSessionId(), dto.getMessage());

        AiChatMessage userMsg = persistMessage(session.getId(), userId, "user", dto.getMessage(), null);

        List<ChatMsg> messages = new ArrayList<>();
        messages.add(ChatMsg.system(aiProps.getSystemPrompt()));

        for (AiChatMessage m : loadHistory(session.getId(), aiProps.getHistoryLimit(), userMsg.getId())) {
            messages.add(new ChatMsg(m.getRole(), m.getContent()));
        }
        messages.add(ChatMsg.user(dto.getMessage()));

        String reply = aiClient.chatCompletion(messages);

        AiChatMessage assistantMsg = persistMessage(session.getId(), userId, "assistant", reply, null);

        session.setLastMessage(truncate(reply, LAST_MSG_PREVIEW));
        session.setLastActiveTime(LocalDateTime.now());
        sessionMapper.updateById(session);

        ChatReplyVO vo = new ChatReplyVO();
        vo.setSessionId(session.getId());
        vo.setUserMessageId(userMsg.getId());
        vo.setAssistantMessageId(assistantMsg.getId());
        vo.setReply(reply);
        vo.setReferencedBooks(Collections.emptyList());
        vo.setCreateTime(assistantMsg.getCreateTime());
        return vo;
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
        return list.stream().map(this::toSessionVO).collect(java.util.stream.Collectors.toList());
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

        List<ChatMessageVO> result = new ArrayList<>(messages.size());
        for (AiChatMessage m : messages) {
            ChatMessageVO vo = new ChatMessageVO();
            vo.setId(m.getId());
            vo.setSessionId(m.getSessionId());
            vo.setRole(m.getRole());
            vo.setContent(m.getContent());
            vo.setCreateTime(m.getCreateTime());
            vo.setReferencedBooks(Collections.emptyList());
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

    private static String truncate(String s, int max) {
        if (s == null) return null;
        return s.length() <= max ? s : s.substring(0, max);
    }
}
