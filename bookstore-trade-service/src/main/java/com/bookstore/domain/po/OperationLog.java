package com.bookstore.domain.po;

import com.baomidou.mybatisplus.annotation.TableName;
import com.bookstore.domain.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("operation_log")
public class OperationLog extends BaseEntity {

    private Long adminUserId;
    private String adminUsername;
    private String actionType;
    private String resourceType;
    private String targetId;
    private String summary;
    private String requestPath;
    private String requestMethod;
    private String ipAddress;
    private Integer success;
    private String errorMsg;
    private Integer durationMs;
}
