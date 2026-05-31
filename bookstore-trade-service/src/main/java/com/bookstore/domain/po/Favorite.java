package com.bookstore.domain.po;

import com.baomidou.mybatisplus.annotation.TableName;
import com.bookstore.domain.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("favorite")
public class Favorite extends BaseEntity {

    private Long userId;
    private Long bookId;
}
