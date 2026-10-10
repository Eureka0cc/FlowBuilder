package com.eureka.flowbuilder.model.dto.billing;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

@Data
public class AdminCreditRechargeRequest implements Serializable {
    private Long userId;
    /** 充值金额（CNY，正数） */
    private BigDecimal amount;
    private String remark;
}
