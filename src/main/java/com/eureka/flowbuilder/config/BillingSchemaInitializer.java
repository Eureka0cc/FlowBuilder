package com.eureka.flowbuilder.config;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Component
public class BillingSchemaInitializer {

    private final DataSource dataSource;
    private final JdbcTemplate jdbcTemplate;

    public BillingSchemaInitializer(DataSource dataSource, JdbcTemplate jdbcTemplate) {
        this.dataSource = dataSource;
        this.jdbcTemplate = jdbcTemplate;
    }

    @PostConstruct
    public void initialize() {
        ResourceDatabasePopulator populator =
                new ResourceDatabasePopulator(new ClassPathResource("sql/billing.sql"));
        populator.execute(dataSource);
        ensureUserBillingColumns();
    }

    private void ensureUserBillingColumns() {
        Set<String> columns = jdbcTemplate.queryForList(
                        """
                                SELECT COLUMN_NAME FROM INFORMATION_SCHEMA.COLUMNS
                                WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'user'
                                """)
                .stream()
                .map(row -> String.valueOf(row.get("COLUMN_NAME")))
                .collect(Collectors.toSet());

        addColumnIfMissing(columns, "planCode",
                "ALTER TABLE `user` ADD COLUMN `planCode` VARCHAR(32) NOT NULL DEFAULT 'free' COMMENT '套餐编码' AFTER `userRole`");
        addColumnIfMissing(columns, "creditBalance",
                "ALTER TABLE `user` ADD COLUMN `creditBalance` DECIMAL(18, 8) NOT NULL DEFAULT 0 COMMENT '积分余额(CNY)' AFTER `planCode`");
        addColumnIfMissing(columns, "creditUpdatedTime",
                "ALTER TABLE `user` ADD COLUMN `creditUpdatedTime` DATETIME NULL COMMENT '余额更新时间' AFTER `creditBalance`");
        addColumnIfMissing(columns, "planExpireTime",
                "ALTER TABLE `user` ADD COLUMN `planExpireTime` DATETIME NULL COMMENT '套餐到期时间' AFTER `creditUpdatedTime`");
        addColumnIfMissing(columns, "monthlyQuotaResetAt",
                "ALTER TABLE `user` ADD COLUMN `monthlyQuotaResetAt` DATETIME NULL COMMENT '月额度上次重置时间' AFTER `planExpireTime`");
    }

    private void addColumnIfMissing(Set<String> columns, String columnName, String ddl) {
        if (columns.contains(columnName)) {
            return;
        }
        jdbcTemplate.execute(ddl);
        columns.add(columnName);
        log.info("Added billing column {}.{}", "user", columnName);
    }
}
