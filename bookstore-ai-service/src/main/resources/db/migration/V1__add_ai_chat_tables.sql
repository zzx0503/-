-- V9__add_ai_chat_tables.sql
-- Filtered for service tables

CREATE TABLE IF NOT EXISTS ai_chat_session (
    id               BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '会话ID',
    user_id          BIGINT NOT NULL COMMENT '用户ID',
    title            VARCHAR(120) NOT NULL DEFAULT '新的对话' COMMENT '会话标题',
    last_message     VARCHAR(500) DEFAULT NULL COMMENT '最近一条消息预览',
    last_active_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '最近活跃时间',
    create_time      DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time      DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted          TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    INDEX idx_user (user_id, last_active_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI聊天会话';

CREATE TABLE IF NOT EXISTS ai_chat_message (
    id            BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '消息ID',
    session_id    BIGINT NOT NULL COMMENT '会话ID',
    user_id       BIGINT NOT NULL COMMENT '用户ID',
    role          VARCHAR(16) NOT NULL COMMENT 'user/assistant/system',
    content       MEDIUMTEXT NOT NULL COMMENT '消息内容',
    token_count   INT DEFAULT NULL COMMENT 'token估算',
    referenced_book_ids VARCHAR(500) DEFAULT NULL COMMENT '回复中引用的图书ID列表(逗号分隔)',
    create_time   DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time   DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted       TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    INDEX idx_session (session_id, id),
    INDEX idx_user (user_id, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI聊天消息';
