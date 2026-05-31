package com.bookstore.domain.po;

import com.baomidou.mybatisplus.annotation.TableName;
import com.bookstore.domain.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("cart_item")
public class CartItem extends BaseEntity {

    private Long userId;
    private Long bookId;
    private Integer quantity;
    private Integer selected;
}
