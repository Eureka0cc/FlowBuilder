package com.eureka.flowbuilder.config;

import jakarta.annotation.PostConstruct;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;

@Component
public class AiUsageSchemaInitializer {

    private final DataSource dataSource;

    public AiUsageSchemaInitializer(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @PostConstruct
    public void initialize() {
        ResourceDatabasePopulator populator =
                new ResourceDatabasePopulator(new ClassPathResource("sql/ai_usage_log.sql"));
        populator.execute(dataSource);
    }
}
