package com.bookstore.aop;

import com.bookstore.context.UserContext;
import com.bookstore.mapper.SearchHistoryMapper;
import com.bookstore.response.PageResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class SearchHistoryAspect {

    private final SearchHistoryMapper searchHistoryMapper;

    @Async("taskExecutor")
    @AfterReturning(value = "@annotation(searchHistory)", returning = "result")
    public void record(JoinPoint joinPoint, com.bookstore.anno.SearchHistory searchHistory, Object result) {
        try {
            com.bookstore.context.CurrentUser cu = UserContext.get();
            Long userId = cu != null ? cu.getUserId() : null;
            if (userId == null) {
                return;
            }

            String keyword = extractKeyword(joinPoint);
            int resultCount = extractResultCount(result);

            if (keyword == null || keyword.isBlank()) {
                return;
            }

            com.bookstore.domain.po.SearchHistory sh = new com.bookstore.domain.po.SearchHistory();
            sh.setUserId(userId);
            sh.setKeyword(keyword.trim());
            sh.setSearchType(searchHistory.type());
            sh.setResultCount(resultCount);
            searchHistoryMapper.insert(sh);
        } catch (Exception e) {
            log.warn("SearchHistory record failed", e);
        }
    }

    private String extractKeyword(JoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String[] paramNames = signature.getParameterNames();
        Object[] args = joinPoint.getArgs();
        for (int i = 0; i < paramNames.length; i++) {
            if ("keyword".equals(paramNames[i]) && args[i] instanceof String) {
                return (String) args[i];
            }
        }
        return null;
    }

    private int extractResultCount(Object result) {
        if (result instanceof PageResult<?> pr) {
            return (int) pr.getTotal();
        }
        return 0;
    }
}
