package com.eureka.flowbuilder.model.vo.billing;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class CreditLedgerVO {
    private Long id;
    private Long userId;
    private BigDecimal changeAmount;
    private BigDecimal changeDisplay;
    private BigDecimal balanceAfter;
    private BigDecimal balanceAfterDisplay;
    private String bizType;
    private String bizId;
    private String remark;
    private Long operatorId;
    private LocalDateTime createTime;
}
