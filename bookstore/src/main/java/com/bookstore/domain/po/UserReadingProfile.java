package com.bookstore.domain.po;

import com.baomidou.mybatisplus.annotation.TableName;
import com.bookstore.domain.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("user_reading_profile")
public class UserReadingProfile extends BaseEntity {

    private Long userId;
    private String profileAnalysis;
    private String profileDataSnapshot;
    private LocalDateTime expireTime;
    private Integer refreshCount;
    private String lastRefreshStatus;
}
