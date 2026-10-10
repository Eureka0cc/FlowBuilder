package com.eureka.flowbuilder.model.vo.billing;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class BillingAccountVO {

    private String planCode;
    private String planName;
    private String planDescription;
    private BigDecimal creditBalance;
    private BigDecimal creditDisplay;
    private BigDecimal monthlyQuota;
    private Integer appLimit;
    private Integer appCount;
    private int chatRate;
    private LocalDateTime planExpireTime;
    private LocalDateTime monthlyQuotaResetAt;
    private LocalDateTime creditUpdatedTime;
    private List<PlanOptionVO> plans;

    @Data
    @Builder
    public static class PlanOptionVO {
        private String planCode;
        private String name;
        private String description;
        private BigDecimal monthlyQuota;
        private BigDecimal monthlyQuotaDisplay;
        private Integer appLimit;
        private int chatRate;
        private BigDecimal signupGift;
        private BigDecimal signupGiftDisplay;
    }
}
