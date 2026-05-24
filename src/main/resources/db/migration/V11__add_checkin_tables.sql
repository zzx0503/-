-- 用户签到记录
CREATE TABLE IF NOT EXISTS user_checkin (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '签到记录ID',
    user_id         BIGINT NOT NULL COMMENT '用户ID',
    checkin_date    DATE NOT NULL COMMENT '签到日期',
    consecutive_days INT NOT NULL DEFAULT 1 COMMENT '连续签到天数',
    reward_amount   DECIMAL(10, 2) NOT NULL DEFAULT 0.00 COMMENT '本次奖励金额',
    create_time     DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted         TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    UNIQUE KEY uk_user_date (user_id, checkin_date),
    INDEX idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户签到记录';

-- 签到奖励规则
CREATE TABLE IF NOT EXISTS checkin_reward_rule (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '规则ID',
    consecutive_days INT NOT NULL COMMENT '连续天数',
    reward_amount   DECIMAL(10, 2) NOT NULL COMMENT '奖励金额',
    bonus_type      VARCHAR(20) DEFAULT 'BASE' COMMENT 'BASE-基础, BONUS-额外',
    description     VARCHAR(100) DEFAULT NULL COMMENT '描述',
    create_time     DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time     DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted         TINYINT NOT NULL DEFAULT 0,
    UNIQUE KEY uk_consecutive_days (consecutive_days)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='签到奖励规则';

-- 初始化奖励规则
INSERT INTO checkin_reward_rule (consecutive_days, reward_amount, bonus_type, description) VALUES
(1, 0.10, 'BASE', '每日签到'),
(7, 1.00, 'BONUS', '连续7天奖励'),
(30, 5.00, 'BONUS', '连续30天奖励')
ON DUPLICATE KEY UPDATE reward_amount = VALUES(reward_amount);
