package com.bookstore.domain.vo.ai;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ChatSessionVO {

    private Long id;
    private String title;
    private String lastMessage;
    private LocalDateTime lastActiveTime;
    private LocalDateTime createTime;
}
