package com.bookstore.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bookstore.domain.po.OperationLog;
import com.bookstore.mapper.OperationLogMapper;
import com.bookstore.response.PageResult;
import com.bookstore.service.OperationLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class OperationLogServiceImpl implements OperationLogService {

    private final OperationLogMapper operationLogMapper;

    @Override
    @Async("taskExecutor")
    public void recordAsync(OperationLog log) {
        try {
            operationLogMapper.insert(log);
        } catch (Exception e) {
            OperationLogServiceImpl.log.warn("operation log insert failed", e);
        }
    }

    @Override
    public PageResult<OperationLog> list(String resourceType,
                                         String actionType,
                                         Long adminUserId,
                                         LocalDateTime from,
                                         LocalDateTime to,
                                         Integer page,
                                         Integer size) {
        LambdaQueryWrapper<OperationLog> w = new LambdaQueryWrapper<OperationLog>()
            .eq(OperationLog::getDeleted, 0)
            .orderByDesc(OperationLog::getCreateTime);
        if (StringUtils.hasText(resourceType)) {
            w.eq(OperationLog::getResourceType, resourceType);
        }
        if (StringUtils.hasText(actionType)) {
            w.eq(OperationLog::getActionType, actionType);
        }
        if (adminUserId != null) {
            w.eq(OperationLog::getAdminUserId, adminUserId);
        }
        if (from != null) {
            w.ge(OperationLog::getCreateTime, from);
        }
        if (to != null) {
            w.le(OperationLog::getCreateTime, to);
        }
        Page<OperationLog> p = operationLogMapper.selectPage(new Page<>(page, size), w);
        return PageResult.of(p.getRecords(), p.getTotal(), p.getCurrent(), p.getSize());
    }
}
