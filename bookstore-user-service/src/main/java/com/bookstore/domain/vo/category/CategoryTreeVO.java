package com.bookstore.domain.vo.category;

import lombok.Data;

import java.util.List;

@Data
public class CategoryTreeVO {

    private Long id;
    private String name;
    private String iconKey;
    private Integer sort;
    private Integer status;
    private List<CategoryTreeVO> children;
}
