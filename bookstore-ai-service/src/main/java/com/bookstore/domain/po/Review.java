package com.bookstore.domain.po;

import com.baomidou.mybatisplus.annotation.TableName;
import com.bookstore.domain.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("review")
public class Review extends BaseEntity {

    private Long userId;
    private Long bookId;
    private Long orderId;
    private Integer rating;
    private String content;
    private String images;
}
