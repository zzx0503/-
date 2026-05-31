-- V8__add_operation_log.sql
-- Filtered for service tables

CREATE TABLE IF NOT EXISTS operation_log (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '日志ID',
    admin_user_id   BIGINT DEFAULT NULL COMMENT '操作管理员ID',
    admin_username  VARCHAR(64) DEFAULT NULL COMMENT '操作管理员用户名快照',
    action_type     VARCHAR(20) NOT NULL COMMENT '操作类型: CREATE/UPDATE/DELETE/ACTION',
    resource_type   VARCHAR(40) NOT NULL COMMENT '资源类型: BOOK/CATEGORY/ORDER/COUPON_TEMPLATE/SECKILL_ACTIVITY/USER',
    target_id       VARCHAR(64) DEFAULT NULL COMMENT '目标资源ID',
    summary         VARCHAR(255) DEFAULT NULL COMMENT '人类可读摘要',
    request_path    VARCHAR(255) NOT NULL COMMENT '请求路径',
    request_method  VARCHAR(10) NOT NULL COMMENT 'HTTP方法',
    ip_address      VARCHAR(64) DEFAULT NULL COMMENT '客户端IP',
    success         TINYINT NOT NULL DEFAULT 1 COMMENT '0失败 1成功',
    error_msg       VARCHAR(500) DEFAULT NULL COMMENT '失败信息',
    duration_ms     INT NOT NULL DEFAULT 0 COMMENT '耗时(ms)',
    create_time     DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted         TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    INDEX idx_admin (admin_user_id),
    INDEX idx_resource (resource_type, target_id),
    INDEX idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='后台操作日志';
