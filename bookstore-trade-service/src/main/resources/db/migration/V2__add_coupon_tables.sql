-- V6__add_coupon_tables.sql
-- Filtered for service tables

CREATE TABLE IF NOT EXISTS coupon_template (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '券模板ID',
    name            VARCHAR(100) NOT NULL COMMENT '券名称',
    type            VARCHAR(20) NOT NULL COMMENT '类型: FULL_REDUCE-满减, DISCOUNT-折扣, AMOUNT-直减',
    threshold       DECIMAL(10, 2) NOT NULL DEFAULT 0.00 COMMENT '使用门槛',
    discount_value  DECIMAL(10, 2) NOT NULL COMMENT '满减金额/折扣率(0-1之间, 0.8表示8折)/直减金额',
    total_count     INT NOT NULL DEFAULT 0 COMMENT '发行总量',
    claimed_count   INT NOT NULL DEFAULT 0 COMMENT '已领取数量',
    valid_from      DATETIME NOT NULL COMMENT '生效时间',
    valid_to        DATETIME NOT NULL COMMENT '失效时间',
    status          VARCHAR(20) NOT NULL DEFAULT 'READY' COMMENT 'READY/RUNNING/ENDED',
    description     VARCHAR(255) DEFAULT NULL COMMENT '说明文案',
    create_time     DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted         TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    INDEX idx_status (status),
    INDEX idx_valid_to (valid_to)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='优惠券模板';

CREATE TABLE IF NOT EXISTS user_coupon (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '用户券ID',
    user_id         BIGINT NOT NULL COMMENT '用户ID',
    template_id     BIGINT NOT NULL COMMENT '券模板ID',
    code            VARCHAR(32) NOT NULL COMMENT '券码',
    status          VARCHAR(20) NOT NULL DEFAULT 'UNUSED' COMMENT 'UNUSED/LOCKED/USED/EXPIRED',
    locked_order_no VARCHAR(32) DEFAULT NULL COMMENT '锁定的订单号',
    used_order_no   VARCHAR(32) DEFAULT NULL COMMENT '使用的订单号',
    used_at         DATETIME DEFAULT NULL COMMENT '使用时间',
    expire_time     DATETIME NOT NULL COMMENT '过期时间(冗余coupon_template.valid_to)',
    create_time     DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '领取时间',
    update_time     DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted         TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    UNIQUE KEY uk_code (code),
    INDEX idx_user_status (user_id, status),
    INDEX idx_template (template_id),
    INDEX idx_locked_order (locked_order_no),
    INDEX idx_used_order (used_order_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户优惠券';

ALTER TABLE order_main ADD COLUMN coupon_id BIGINT DEFAULT NULL COMMENT '使用的用户券ID' AFTER discount_amount;

ALTER TABLE order_main ADD INDEX idx_coupon_id (coupon_id);
