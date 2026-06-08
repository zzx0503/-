package com.bookstore.domain.po;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.bookstore.domain.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("book")
public class Book extends BaseEntity {

    private String isbn;
    private String title;
    private String subtitle;
    private String author;
    private String translator;
    private String publisher;
    private Long categoryId;
    private String coverKey;
    private BigDecimal price;
    private BigDecimal originalPrice;
    private Integer stock;
    private Integer salesCount;
    private BigDecimal rating;
    private String description;

    @TableField("description_vector")
    private String descriptionVector;

    private LocalDate publishDate;
    private Integer status;
}
