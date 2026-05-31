package com.bookstore.domain.po;

import com.baomidou.mybatisplus.annotation.TableName;
import com.bookstore.domain.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ai_chat_session")
public class AiChatSession extends BaseEntity {

    private Long userId;
    private String title;
    private String lastMessage;
    private LocalDateTime lastActiveTime;
}
