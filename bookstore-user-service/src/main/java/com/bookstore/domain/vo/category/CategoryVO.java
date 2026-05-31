package com.bookstore.domain.vo.category;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CategoryVO {

    private Long id;
    private String name;
    private Long parentId;
    private String iconKey;
    private Integer sort;
    private Integer status;
    private LocalDateTime createTime;
}
