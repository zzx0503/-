-- 秒杀活动
CREATE TABLE IF NOT EXISTS seckill_activity (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '活动ID',
    book_id         BIGINT NOT NULL COMMENT '关联图书ID',
    seckill_price   DECIMAL(10, 2) NOT NULL COMMENT '秒杀价',
    original_price  DECIMAL(10, 2) NOT NULL COMMENT '原价快照',
    total_stock     INT NOT NULL DEFAULT 0 COMMENT '秒杀总量',
    sold_count      INT NOT NULL DEFAULT 0 COMMENT '已售出',
    per_user_limit  INT NOT NULL DEFAULT 1 COMMENT '每用户限购',
    start_time      DATETIME NOT NULL COMMENT '开始时间',
    end_time        DATETIME NOT NULL COMMENT '结束时间',
    status          VARCHAR(20) NOT NULL DEFAULT 'READY' COMMENT 'READY/RUNNING/ENDED',
    title           VARCHAR(100) DEFAULT NULL COMMENT '活动标题',
    create_time     DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted         TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    INDEX idx_book (book_id),
    INDEX idx_status (status),
    INDEX idx_time_range (start_time, end_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='秒杀活动';

-- 秒杀订单
CREATE TABLE IF NOT EXISTS seckill_order (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '秒杀订单ID',
    order_no        VARCHAR(32) NOT NULL COMMENT '订单号',
    user_id         BIGINT NOT NULL COMMENT '用户ID',
    activity_id     BIGINT NOT NULL COMMENT '活动ID',
    book_id         BIGINT NOT NULL COMMENT '图书ID',
    quantity        INT NOT NULL DEFAULT 1 COMMENT '数量',
    seckill_price   DECIMAL(10, 2) NOT NULL COMMENT '秒杀价快照',
    total_amount    DECIMAL(10, 2) NOT NULL COMMENT '订单金额',
    status          VARCHAR(20) NOT NULL DEFAULT 'PENDING_PAY' COMMENT 'PENDING_PAY/PAID/CANCELLED/EXPIRED',
    pay_time        DATETIME DEFAULT NULL COMMENT '支付时间',
    expire_time     DATETIME NOT NULL COMMENT '订单过期时间(创建后5分钟)',
    address_snapshot JSON DEFAULT NULL COMMENT '收货地址快照',
    create_time     DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted         TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    UNIQUE KEY uk_order_no (order_no),
    INDEX idx_user_status (user_id, status),
    INDEX idx_activity (activity_id),
    INDEX idx_user_activity (user_id, activity_id),
    INDEX idx_expire (status, expire_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='秒杀订单';
