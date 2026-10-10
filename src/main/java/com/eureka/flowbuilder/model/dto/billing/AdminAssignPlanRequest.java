package com.eureka.flowbuilder.model.dto.billing;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class AdminAssignPlanRequest implements Serializable {
    private Long userId;
    private String planCode;
    /** 套餐到期时间，空表示不过期 */
    private LocalDateTime planExpireTime;
    /** 是否立即按新套餐发放/重置月额度 */
    private Boolean resetMonthlyQuota;
}
