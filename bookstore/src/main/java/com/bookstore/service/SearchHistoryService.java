package com.bookstore.service;

import com.bookstore.domain.po.SearchHistory;

import java.util.List;

public interface SearchHistoryService {

    List<SearchHistory> listRecent(Long userId, Integer limit);

    void clear(Long userId);
}
