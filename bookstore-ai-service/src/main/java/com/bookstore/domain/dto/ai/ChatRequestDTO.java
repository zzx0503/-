package com.bookstore.domain.dto.ai;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ChatRequestDTO {

    private Long sessionId;

    @NotBlank(message = "消息内容不能为空")
    @Size(max = 2000, message = "消息内容过长")
    private String message;
}
