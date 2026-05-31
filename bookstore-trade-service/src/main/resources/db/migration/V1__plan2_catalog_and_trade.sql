-- V2__plan2_catalog_and_trade.sql
-- Filtered for service tables

CREATE TABLE IF NOT EXISTS `cart_item` (
    `id`          BIGINT      NOT NULL AUTO_INCREMENT,
    `user_id`     BIGINT      NOT NULL COMMENT '用户ID',
    `book_id`     BIGINT      NOT NULL COMMENT '图书ID',
    `quantity`    INT         NOT NULL DEFAULT 1 COMMENT '数量',
    `selected`    TINYINT(1)  NOT NULL DEFAULT 1 COMMENT '0未勾选 1勾选',
    `create_time` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`     TINYINT(1)  NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_cart_user_book` (`user_id`, `book_id`),
    KEY `idx_cart_user` (`user_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '购物车项';

CREATE TABLE IF NOT EXISTS `order_main` (
    `id`                BIGINT          NOT NULL AUTO_INCREMENT,
    `order_no`          VARCHAR(32)     NOT NULL COMMENT '订单号',
    `user_id`           BIGINT          NOT NULL COMMENT '用户ID',
    `total_amount`      DECIMAL(10,2)   NOT NULL COMMENT '订单总金额',
    `pay_amount`        DECIMAL(10,2)   NOT NULL COMMENT '实付金额',
    `discount_amount`   DECIMAL(10,2)   NOT NULL DEFAULT 0.00 COMMENT '优惠金额',
    `pay_method`        VARCHAR(20)              DEFAULT NULL COMMENT '支付方式 MOCK/ALIPAY/WECHAT',
    `pay_time`          DATETIME                 DEFAULT NULL,
    `status`            VARCHAR(20)     NOT NULL DEFAULT 'PENDING_PAY' COMMENT 'PENDING_PAY/PAID/SHIPPED/COMPLETED/CANCELLED',
    `address_snapshot`  JSON            NOT NULL COMMENT '收货地址快照',
    `remark`            VARCHAR(255)             DEFAULT NULL COMMENT '用户备注',
    `ship_time`         DATETIME                 DEFAULT NULL,
    `complete_time`     DATETIME                 DEFAULT NULL,
    `create_time`       DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`       DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`           TINYINT(1)      NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_order_no` (`order_no`),
    KEY `idx_order_user` (`user_id`),
    KEY `idx_order_status` (`status`),
    KEY `idx_order_create` (`create_time`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '订单主表';

CREATE TABLE IF NOT EXISTS `order_item` (
    `id`          BIGINT          NOT NULL AUTO_INCREMENT,
    `order_id`    BIGINT          NOT NULL COMMENT '订单ID',
    `book_id`     BIGINT          NOT NULL COMMENT '图书ID',
    `book_title`  VARCHAR(200)    NOT NULL COMMENT '图书标题快照',
    `book_cover`  VARCHAR(255)             DEFAULT NULL COMMENT '图书封面快照',
    `unit_price`  DECIMAL(10,2)   NOT NULL COMMENT '下单时单价快照',
    `quantity`    INT             NOT NULL COMMENT '数量',
    `subtotal`    DECIMAL(10,2)   NOT NULL COMMENT '小计',
    `create_time` DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`     TINYINT(1)      NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    KEY `idx_oi_order` (`order_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '订单明细';
