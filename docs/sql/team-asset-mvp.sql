-- Team asset MVP (MySQL 8.x). Entity and column names intentionally use camelCase.

CREATE TABLE IF NOT EXISTS team (
    id BIGINT NOT NULL COMMENT '雪花 id',
    name VARCHAR(80) NOT NULL,
    ownerId BIGINT NOT NULL,
    isPersonal TINYINT NOT NULL DEFAULT 0,
    createTime DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updateTime DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    isDelete TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_team_ownerId (ownerId),
    KEY idx_team_personal (ownerId, isPersonal, isDelete)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS team_member (
    id BIGINT NOT NULL COMMENT '雪花 id',
    teamId BIGINT NOT NULL,
    userId BIGINT NOT NULL,
    role VARCHAR(16) NOT NULL COMMENT 'OWNER/EDITOR/VIEWER',
    createTime DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updateTime DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    isDelete TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_team_member (teamId, userId),
    KEY idx_team_member_userId (userId, isDelete)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS team_asset (
    id BIGINT NOT NULL COMMENT '雪花 id',
    teamId BIGINT NOT NULL,
    uploaderId BIGINT NOT NULL,
    name VARCHAR(120) NOT NULL,
    url VARCHAR(1024) DEFAULT NULL,
    tosKey VARCHAR(512) DEFAULT NULL,
    contentType VARCHAR(100) NOT NULL,
    size BIGINT NOT NULL,
    width INT NOT NULL,
    height INT NOT NULL,
    tags VARCHAR(500) DEFAULT NULL,
    createTime DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updateTime DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    isDelete TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_team_asset_teamId (teamId, isDelete, createTime),
    KEY idx_team_asset_name (teamId, name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

ALTER TABLE app
    ADD COLUMN teamId BIGINT NULL COMMENT '所属团队 id，历史应用允许为空' AFTER userId,
    ADD KEY idx_app_teamId (teamId, isDelete, createTime);
