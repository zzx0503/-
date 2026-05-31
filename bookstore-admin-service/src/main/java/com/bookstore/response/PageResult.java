package com.bookstore.response;

import lombok.Data;

import java.util.List;

@Data
public class PageResult<T> {
    private List<T> list;
    private long total;
    private int pageNum;
    private int pageSize;
    private int pages;

    public static <T> PageResult<T> of(List<T> list, long total, long pageNum, long pageSize) {
        PageResult<T> p = new PageResult<>();
        p.list = list;
        p.total = total;
        p.pageNum = (int) pageNum;
        p.pageSize = (int) pageSize;
        p.pages = pageSize == 0 ? 0 : (int) ((total + pageSize - 1) / pageSize);
        return p;
    }
}
