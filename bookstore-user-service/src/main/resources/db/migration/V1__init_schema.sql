-- V1__init_schema.sql
-- Filtered for service tables

CREATE TABLE IF NOT EXISTS `user` (
    `id`             BIGINT       NOT NULL AUTO_INCREMENT,
    `username`       VARCHAR(50)  NOT NULL,
    `password_hash`  VARCHAR(100) NOT NULL COMMENT 'BCrypt encoded',
    `nickname`       VARCHAR(50)           DEFAULT NULL,
    `avatar_key`     VARCHAR(255)          DEFAULT NULL COMMENT 'OSS object key, e.g. avatars/1.jpg',
    `email`          VARCHAR(100)          DEFAULT NULL,
    `phone`          VARCHAR(20)           DEFAULT NULL,
    `gender`         TINYINT               DEFAULT 2 COMMENT '0 nv / 1 nan / 2 unknown',
    `birthday`       DATE                  DEFAULT NULL,
    `role`           VARCHAR(20)  NOT NULL DEFAULT 'USER' COMMENT 'USER / ADMIN',
    `status`         TINYINT      NOT NULL DEFAULT 1 COMMENT '0 disabled / 1 enabled',
    `create_time`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`        TINYINT(1)   NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_username` (`username`),
    UNIQUE KEY `uk_user_phone` (`phone`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = 'user';

CREATE TABLE IF NOT EXISTS `address` (
    `id`              BIGINT       NOT NULL AUTO_INCREMENT,
    `user_id`         BIGINT       NOT NULL,
    `receiver`        VARCHAR(50)  NOT NULL,
    `phone`           VARCHAR(20)  NOT NULL,
    `province`        VARCHAR(50)  NOT NULL,
    `city`            VARCHAR(50)  NOT NULL,
    `district`        VARCHAR(50)  NOT NULL,
    `detail_address`  VARCHAR(255) NOT NULL,
    `is_default`      TINYINT(1)   NOT NULL DEFAULT 0,
    `create_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`         TINYINT(1)   NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    KEY `idx_address_user` (`user_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = 'address';

INSERT INTO `user` (`username`, `password_hash`, `nickname`, `role`, `status`)
VALUES ('admin',
        '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy',
        'system admin', 'ADMIN', 1);
