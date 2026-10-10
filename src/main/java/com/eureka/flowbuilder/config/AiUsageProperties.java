package com.eureka.flowbuilder.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@Data
@Component
@ConfigurationProperties(prefix = "ai.usage")
public class AiUsageProperties {

    private String currency = "CNY";
    private Map<String, ModelPrice> models = new HashMap<>();

    @Data
    public static class ModelPrice {
        private BigDecimal inputPricePerMillion;
        private BigDecimal outputPricePerMillion;
    }
}
