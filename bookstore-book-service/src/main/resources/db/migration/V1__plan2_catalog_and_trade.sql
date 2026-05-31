-- V2__plan2_catalog_and_trade.sql
-- Filtered for service tables

CREATE TABLE IF NOT EXISTS `category` (
    `id`          BIGINT      NOT NULL AUTO_INCREMENT,
    `name`        VARCHAR(50) NOT NULL COMMENT '分类名称',
    `parent_id`   BIGINT      NOT NULL DEFAULT 0 COMMENT '父分类ID，0=一级分类',
    `icon_key`    VARCHAR(255)         DEFAULT NULL COMMENT 'OSS图标key',
    `sort`        INT         NOT NULL DEFAULT 0 COMMENT '排序，升序',
    `status`      TINYINT(1)  NOT NULL DEFAULT 1 COMMENT '0禁用 1启用',
    `create_time` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`     TINYINT(1)  NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    KEY `idx_category_parent` (`parent_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '图书分类';

CREATE TABLE IF NOT EXISTS `book` (
    `id`                   BIGINT          NOT NULL AUTO_INCREMENT,
    `isbn`                 VARCHAR(20)     NOT NULL COMMENT 'ISBN',
    `title`                VARCHAR(200)    NOT NULL COMMENT '书名',
    `subtitle`             VARCHAR(200)             DEFAULT NULL COMMENT '副标题',
    `author`               VARCHAR(100)    NOT NULL COMMENT '作者',
    `translator`           VARCHAR(100)             DEFAULT NULL COMMENT '译者',
    `publisher`            VARCHAR(100)             DEFAULT NULL COMMENT '出版社',
    `category_id`          BIGINT          NOT NULL COMMENT '所属分类ID',
    `cover_key`            VARCHAR(255)             DEFAULT NULL COMMENT '封面OSS key',
    `price`                DECIMAL(10,2)   NOT NULL COMMENT '售价',
    `original_price`       DECIMAL(10,2)   NOT NULL COMMENT '原价',
    `stock`                INT             NOT NULL DEFAULT 0 COMMENT '库存',
    `sales_count`          INT             NOT NULL DEFAULT 0 COMMENT '销量',
    `rating`               DECIMAL(2,1)             DEFAULT 5.0 COMMENT '平均评分',
    `description`          TEXT                     DEFAULT NULL COMMENT '简介',
    `description_vector`   JSON                     DEFAULT NULL COMMENT '描述向量，Plan 3 AI推荐用',
    `publish_date`         DATE                     DEFAULT NULL COMMENT '出版日期',
    `status`               TINYINT(1)      NOT NULL DEFAULT 1 COMMENT '0下架 1上架',
    `create_time`          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`              TINYINT(1)      NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_book_isbn` (`isbn`),
    KEY `idx_book_category` (`category_id`),
    KEY `idx_book_status` (`status`),
    KEY `idx_book_publish` (`publish_date`),
    FULLTEXT KEY `ft_book_title` (`title`, `subtitle`, `author`) WITH PARSER ngram
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '图书';

CREATE TABLE IF NOT EXISTS `favorite` (
    `id`          BIGINT      NOT NULL AUTO_INCREMENT,
    `user_id`     BIGINT      NOT NULL COMMENT '用户ID',
    `book_id`     BIGINT      NOT NULL COMMENT '图书ID',
    `create_time` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`     TINYINT(1)  NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_fav_user_book` (`user_id`, `book_id`),
    KEY `idx_fav_user_create` (`user_id`, `create_time`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '收藏';

CREATE TABLE IF NOT EXISTS `review` (
    `id`          BIGINT      NOT NULL AUTO_INCREMENT,
    `user_id`     BIGINT      NOT NULL COMMENT '用户ID',
    `book_id`     BIGINT      NOT NULL COMMENT '图书ID',
    `order_id`    BIGINT      NOT NULL COMMENT '关联订单ID',
    `rating`      TINYINT     NOT NULL DEFAULT 5 COMMENT '评分 1-5',
    `content`     TEXT        NOT NULL COMMENT '评价内容',
    `images`      JSON                 DEFAULT NULL COMMENT '评价图片OSS key数组',
    `create_time` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`     TINYINT(1)  NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_review_user_book_order` (`user_id`, `book_id`, `order_id`),
    KEY `idx_review_book_create` (`book_id`, `create_time`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '评价';

CREATE TABLE IF NOT EXISTS `search_history` (
    `id`           BIGINT       NOT NULL AUTO_INCREMENT,
    `user_id`      BIGINT       NOT NULL COMMENT '用户ID',
    `keyword`      VARCHAR(100) NOT NULL COMMENT '搜索关键词',
    `search_type`  VARCHAR(20)  NOT NULL DEFAULT 'TEXT' COMMENT 'TEXT/CATEGORY/IMAGE',
    `result_count` INT          NOT NULL DEFAULT 0 COMMENT '搜索结果数',
    `create_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`      TINYINT(1)   NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    KEY `idx_sh_user_create` (`user_id`, `create_time`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '搜索历史';

INSERT INTO `category` (`name`, `parent_id`, `sort`) VALUES
    ('文学', 0, 1),
    ('历史', 0, 2),
    ('计算机', 0, 3),
    ('经济', 0, 4),
    ('自然科学', 0, 5),
    ('教育', 0, 6),
    ('童书', 0, 7),
    ('外语', 0, 8),
    ('艺术', 0, 9);
