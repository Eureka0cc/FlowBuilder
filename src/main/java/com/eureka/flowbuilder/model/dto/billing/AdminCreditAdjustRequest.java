package com.eureka.flowbuilder.model.dto.billing;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

@Data
public class AdminCreditAdjustRequest implements Serializable {
    private Long userId;
    /** 调账金额（CNY，可正可负） */
    private BigDecimal amount;
    private String remark;
}
