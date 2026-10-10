-- 实验：查询走了索引但还是慢
-- 场景：在选择性极低的字段（gender）上建索引，导致索引效果差

-- 1. 创建表
CREATE TABLE IF NOT EXISTS user_experiment (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_name VARCHAR(50) NOT NULL,
    password VARCHAR(100) NOT NULL,
    age TINYINT UNSIGNED,
    gender ENUM('male', 'female', 'unknown') NOT NULL DEFAULT 'unknown',
    phone VARCHAR(20) UNIQUE,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    modify_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_admin TINYINT(1) NOT NULL DEFAULT 0,

    -- 关键：在选择性极低的字段上建索引
    INDEX idx_gender (gender),
    -- 复合索引：gender + age（SELECT * 时仍需回表）
    INDEX idx_gender_age (gender, age),
    -- 覆盖索引：包含常用查询字段，避免回表
    INDEX idx_gender_age_covering (gender, age, user_name, phone),
    -- 为了对比，也建一个选择性高的索引
    INDEX idx_phone (phone)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 2. 插入大量数据（约100万条）
-- 为了演示效果，数据要均匀分布在3个 gender 值上

-- 使用存储过程批量插入
DELIMITER $$

DROP PROCEDURE IF EXISTS insert_users$$

CREATE PROCEDURE insert_users(IN batch_size INT, IN total_count INT)
BEGIN
    DECLARE i INT DEFAULT 0;
    DECLARE batch_num INT DEFAULT 0;
    DECLARE max_batch INT DEFAULT total_count / batch_size;

    WHILE batch_num < max_batch DO
        INSERT INTO user_experiment (user_name, password, age, gender, phone, is_admin)
        SELECT
            CONCAT('user_', (batch_num * batch_size + n.seq)),
            CONCAT('pwd_', MD5(CONCAT(batch_num, n.seq))),
            FLOOR(18 + RAND() * 50),
            CASE (n.seq % 3)
                WHEN 0 THEN 'male'
                WHEN 1 THEN 'female'
                ELSE 'unknown'
            END,
            CONCAT('138', LPAD((batch_num * batch_size + n.seq), 8, '0')),
            CASE (n.seq % 100) WHEN 0 THEN 1 ELSE 0 END
        FROM (
            SELECT 1 AS seq UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5
            UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9 UNION SELECT 10
            UNION SELECT 11 UNION SELECT 12 UNION SELECT 13 UNION SELECT 14 UNION SELECT 15
            UNION SELECT 16 UNION SELECT 17 UNION SELECT 18 UNION SELECT 19 UNION SELECT 20
            UNION SELECT 21 UNION SELECT 22 UNION SELECT 23 UNION SELECT 24 UNION SELECT 25
            UNION SELECT 26 UNION SELECT 27 UNION SELECT 28 UNION SELECT 29 UNION SELECT 30
            UNION SELECT 31 UNION SELECT 32 UNION SELECT 33 UNION SELECT 34 UNION SELECT 35
            UNION SELECT 36 UNION SELECT 37 UNION SELECT 38 UNION SELECT 39 UNION SELECT 40
            UNION SELECT 41 UNION SELECT 42 UNION SELECT 43 UNION SELECT 44 UNION SELECT 45
            UNION SELECT 46 UNION SELECT 47 UNION SELECT 48 UNION SELECT 49 UNION SELECT 50
        ) n;

        SET batch_num = batch_num + 1;
        -- 每10批提交一次，避免锁表时间过长
        IF batch_num % 10 = 0 THEN
            COMMIT;
        END IF;
    END WHILE;

    COMMIT;
END$$

DELIMITER ;

-- 3. 执行插入（约100万条数据，每批50条，20000批）
-- 这可能需要几分钟，可以根据实际情况调整
CALL insert_users(50, 1000000);

-- 4. 验证数据分布
SELECT gender, COUNT(*) as count
FROM user_experiment
GROUP BY gender;

-- 5. 测试查询：走了索引但很慢
-- 这个查询会使用 idx_gender 索引，但由于选择性低，效果很差
EXPLAIN SELECT * FROM user_experiment WHERE gender = 'male';

-- 实际执行（观察执行时间）
SELECT * FROM user_experiment WHERE gender = 'male' LIMIT 1000;

-- 6. 对比：选择性高的索引效果好
EXPLAIN SELECT * FROM user_experiment WHERE phone = '13812345678';

-- 7. 强制全表扫描（可能反而更快）
SELECT * FROM user_experiment IGNORE INDEX(idx_gender) WHERE gender = 'male' LIMIT 1000;

-- 清理实验数据（可选）
-- DROP PROCEDURE IF EXISTS insert_users;
-- DROP TABLE IF EXISTS user_experiment;