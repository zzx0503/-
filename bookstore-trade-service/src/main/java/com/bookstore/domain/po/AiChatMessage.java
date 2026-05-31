package com.bookstore.domain.po;

import com.baomidou.mybatisplus.annotation.TableName;
import com.bookstore.domain.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ai_chat_message")
public class AiChatMessage extends BaseEntity {

    private Long sessionId;
    private Long userId;
    private String role;
    private String content;
    private Integer tokenCount;
    private String referencedBookIds;
}
