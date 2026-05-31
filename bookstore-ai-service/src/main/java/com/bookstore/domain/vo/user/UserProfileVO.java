package com.bookstore.domain.vo.user;

import lombok.Data;

import java.time.LocalDate;

@Data
public class UserProfileVO {

    private Long id;
    private String username;
    private String phone;
    private String email;
    private String nickname;
    private String avatarUrl;
    private Integer gender;
    private LocalDate birthday;
    private String role;
}
