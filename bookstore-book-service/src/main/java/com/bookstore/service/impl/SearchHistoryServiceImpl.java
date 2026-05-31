package com.bookstore.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bookstore.domain.po.SearchHistory;
import com.bookstore.mapper.SearchHistoryMapper;
import com.bookstore.service.SearchHistoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SearchHistoryServiceImpl implements SearchHistoryService {

    private final SearchHistoryMapper searchHistoryMapper;

    @Override
    public List<SearchHistory> listRecent(Long userId, Integer limit) {
        return searchHistoryMapper.selectList(
            new LambdaQueryWrapper<SearchHistory>()
                .eq(SearchHistory::getUserId, userId)
                .eq(SearchHistory::getDeleted, 0)
                .orderByDesc(SearchHistory::getCreateTime)
                .last("LIMIT " + limit)
        );
    }

    @Override
    public void clear(Long userId) {
        searchHistoryMapper.delete(
            new LambdaQueryWrapper<SearchHistory>()
                .eq(SearchHistory::getUserId, userId)
        );
    }
}
