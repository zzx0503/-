package com.bookstore.domain.vo.ai;

import com.bookstore.domain.vo.book.BookListVO;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class ChatMessageVO {

    private Long id;
    private Long sessionId;
    private String role;
    private String content;
    private List<BookListVO> referencedBooks;
    private LocalDateTime createTime;
}
