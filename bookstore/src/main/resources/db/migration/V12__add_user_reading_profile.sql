-- 用户阅读画像缓存：每7天由定时任务刷新
CREATE TABLE IF NOT EXISTS user_reading_profile (
    id                    BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'ID',
    user_id               BIGINT NOT NULL COMMENT '用户ID',
    profile_analysis      TEXT COMMENT 'LLM分析的用户画像结论',
    profile_data_snapshot TEXT COMMENT '原始数据快照（生成时的收藏/购物车/订单数据）',
    expire_time           DATETIME NOT NULL COMMENT '过期时间，超过需重新生成',
    refresh_count         INT NOT NULL DEFAULT 0 COMMENT '累计刷新次数',
    last_refresh_status   VARCHAR(32) DEFAULT 'PENDING' COMMENT 'PENDING/SUCCESS/FAILED',
    create_time           DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time           DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted               TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    UNIQUE KEY uk_user_id (user_id),
    INDEX idx_expire (expire_time, deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户阅读画像缓存';
