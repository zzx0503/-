package com.bookstore.controller.admin;

import com.bookstore.anno.AdminRequired;
import com.bookstore.domain.po.OperationLog;
import com.bookstore.response.PageResult;
import com.bookstore.response.Result;
import com.bookstore.service.OperationLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@Tag(name = "操作日志", description = "管理端操作日志")
@RestController
@RequestMapping("/api/admin/operation-logs")
@RequiredArgsConstructor
@AdminRequired
public class OperationLogAdminController {

    private final OperationLogService operationLogService;

    @Operation(summary = "查询操作日志列表")
    @GetMapping
    public Result<PageResult<OperationLog>> list(
            @RequestParam(required = false) String resourceType,
            @RequestParam(required = false) String actionType,
            @RequestParam(required = false) Long adminUserId,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer size) {
        return Result.success(operationLogService.list(resourceType, actionType, adminUserId, from, to, page, size));
    }
}
