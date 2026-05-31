package com.bookstore.domain.po;

import com.baomidou.mybatisplus.annotation.TableName;
import com.bookstore.domain.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("search_history")
public class SearchHistory extends BaseEntity {

    private Long userId;
    private String keyword;
    private String searchType;
    private Integer resultCount;
}
