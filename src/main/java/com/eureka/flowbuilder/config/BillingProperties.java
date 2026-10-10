package com.eureka.flowbuilder.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

@Data
@Component
@ConfigurationProperties(prefix = "billing")
public class BillingProperties {

    /**
     * 预检最低余额（CNY）
     */
    private BigDecimal minReserve = new BigDecimal("0.01");

    /**
     * 当模型单价未配置或估算费用为 0 时的保底扣减（CNY）
     */
    private BigDecimal minConsume = new BigDecimal("0.01");

    /**
     * 展示换算：1 积分 = ¥0.01
     */
    private BigDecimal creditDisplayFactor = new BigDecimal("100");

    private Map<String, PlanConfig> plans = defaultPlans();

    @Data
    public static class PlanConfig {
        private String name;
        private String description;
        private BigDecimal signupGift = BigDecimal.ZERO;
        private BigDecimal monthlyQuota = BigDecimal.ZERO;
        private Integer appLimit;
        private int chatRate = 5;
        private int chatRateIntervalSeconds = 60;
    }

    public PlanConfig getPlan(String planCode) {
        if (planCode == null || planCode.isBlank()) {
            return plans.getOrDefault("free", defaultPlans().get("free"));
        }
        return plans.getOrDefault(planCode, plans.getOrDefault("free", defaultPlans().get("free")));
    }

    private static Map<String, PlanConfig> defaultPlans() {
        Map<String, PlanConfig> defaults = new LinkedHashMap<>();

        PlanConfig free = new PlanConfig();
        free.setName("免费版");
        free.setDescription("适合个人试用，含基础生成额度");
        free.setSignupGift(new BigDecimal("1.00"));
        free.setMonthlyQuota(new BigDecimal("1.00"));
        free.setAppLimit(3);
        free.setChatRate(5);
        free.setChatRateIntervalSeconds(60);
        defaults.put("free", free);

        PlanConfig pro = new PlanConfig();
        pro.setName("专业版");
        pro.setDescription("适合独立开发者与小团队");
        pro.setSignupGift(BigDecimal.ZERO);
        pro.setMonthlyQuota(new BigDecimal("20.00"));
        pro.setAppLimit(50);
        pro.setChatRate(20);
        pro.setChatRateIntervalSeconds(60);
        defaults.put("pro", pro);

        PlanConfig enterprise = new PlanConfig();
        enterprise.setName("企业版");
        enterprise.setDescription("适合企业团队，更高额度与限流");
        enterprise.setSignupGift(BigDecimal.ZERO);
        enterprise.setMonthlyQuota(new BigDecimal("200.00"));
        enterprise.setAppLimit(null);
        enterprise.setChatRate(60);
        enterprise.setChatRateIntervalSeconds(60);
        defaults.put("enterprise", enterprise);

        return defaults;
    }
}
