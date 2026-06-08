package com.bookstore.service;

import com.bookstore.domain.po.OperationLog;
import com.bookstore.response.PageResult;

import java.time.LocalDateTime;

public interface OperationLogService {

    void recordAsync(OperationLog log);

    PageResult<OperationLog> list(String resourceType,
                                  String actionType,
                                  Long adminUserId,
                                  LocalDateTime from,
                                  LocalDateTime to,
                                  Integer page,
                                  Integer size);
}
