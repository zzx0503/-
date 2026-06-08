package com.bookstore.service;

import com.bookstore.domain.dto.ai.ChatRequestDTO;
import com.bookstore.domain.dto.ai.RenameSessionDTO;
import com.bookstore.domain.vo.ai.ChatMessageVO;
import com.bookstore.domain.vo.ai.ChatReplyVO;
import com.bookstore.domain.vo.ai.ChatSessionVO;

import java.util.List;

public interface AiChatService {

    ChatReplyVO chat(Long userId, ChatRequestDTO dto);

    List<ChatSessionVO> listSessions(Long userId);

    ChatSessionVO createSession(Long userId, String title);

    void renameSession(Long userId, Long sessionId, RenameSessionDTO dto);

    void deleteSession(Long userId, Long sessionId);

    List<ChatMessageVO> listMessages(Long userId, Long sessionId);
}
