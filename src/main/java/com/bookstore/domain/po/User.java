package com.bookstore.domain.po;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.bookstore.domain.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("user")
public class User extends BaseEntity {

    private String username;
    private String phone;
    private String email;

    @TableField("password_hash")
    private String passwordHash;

    private String nickname;

    @TableField("avatar_key")
    private String avatarKey;

    private Integer gender;

    private LocalDate birthday;

    private String role;

    private Integer status;

    @TableField("wallet_balance")
    private java.math.BigDecimal walletBalance;
}
