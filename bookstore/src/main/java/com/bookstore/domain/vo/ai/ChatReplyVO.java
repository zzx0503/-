package com.bookstore.domain.vo.ai;

import com.bookstore.domain.vo.book.BookListVO;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class ChatReplyVO {

    private Long sessionId;
    private Long userMessageId;
    private Long assistantMessageId;
    private String reply;
    private List<BookListVO> referencedBooks;
    private LocalDateTime createTime;
}
