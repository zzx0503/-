package com.bookstore.domain.vo.review;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class ReviewVO {

    private Long id;
    private Long userId;
    private String userNickname;
    private String userAvatar;
    private Long bookId;
    private Long orderId;
    private Integer rating;
    private String content;
    private List<String> images;
    private LocalDateTime createTime;
}
