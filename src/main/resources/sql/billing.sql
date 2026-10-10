CREATE TABLE IF NOT EXISTS `credit_ledger` (
    `id` BIGINT NOT NULL,
    `userId` BIGINT NOT NULL,
    `changeAmount` DECIMAL(18, 8) NOT NULL COMMENT '变动金额(CNY)，正增负减',
    `balanceAfter` DECIMAL(18, 8) NOT NULL COMMENT '变动后余额',
    `bizType` VARCHAR(32) NOT NULL COMMENT 'RECHARGE/CONSUME/GIFT/ADJUST/MONTHLY_RESET',
    `bizId` VARCHAR(64) NULL COMMENT '业务单号，防重复扣减',
    `remark` VARCHAR(500) NULL,
    `operatorId` BIGINT NULL COMMENT '操作人，系统扣减可为空',
    `createTime` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_credit_ledger_biz` (`bizType`, `bizId`),
    KEY `idx_credit_ledger_user_time` (`userId`, `createTime`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
