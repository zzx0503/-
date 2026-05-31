-- V4__wallet_and_balance.sql
-- Filtered for service tables

ALTER TABLE user ADD COLUMN wallet_balance DECIMAL(10, 2) NOT NULL DEFAULT 0.00 COMMENT '钱包余额' AFTER status;

CREATE TABLE IF NOT EXISTS wallet_transaction (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '流水ID',
    user_id         BIGINT NOT NULL COMMENT '用户ID',
    order_no        VARCHAR(32) DEFAULT NULL COMMENT '关联订单号',
    type            VARCHAR(20) NOT NULL COMMENT '类型: RECHARGE-充值, PAY-支付, REFUND-退款',
    amount          DECIMAL(10, 2) NOT NULL COMMENT '变动金额(正数)',
    balance_before  DECIMAL(10, 2) NOT NULL COMMENT '变动前余额',
    balance_after   DECIMAL(10, 2) NOT NULL COMMENT '变动后余额',
    remark          VARCHAR(255) DEFAULT NULL COMMENT '备注',
    create_time     DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted         TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    INDEX idx_user_id (user_id),
    INDEX idx_order_no (order_no),
    INDEX idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='钱包交易流水';

ALTER TABLE wallet_transaction
    ADD COLUMN update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间' AFTER create_time,
    ADD COLUMN deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除' AFTER update_time;
