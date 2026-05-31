package com.bookstore.domain.vo.auth;

import lombok.Data;

@Data
public class UserBriefVO {

    private Long id;
    private String username;
    private String nickname;
    private String avatarUrl;
    private String role;
}
