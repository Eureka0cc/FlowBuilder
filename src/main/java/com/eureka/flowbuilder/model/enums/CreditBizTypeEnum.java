package com.eureka.flowbuilder.model.enums;

import lombok.Getter;

@Getter
public enum CreditBizTypeEnum {
    RECHARGE("RECHARGE", "充值"),
    CONSUME("CONSUME", "消耗"),
    GIFT("GIFT", "赠送"),
    ADJUST("ADJUST", "调账"),
    MONTHLY_RESET("MONTHLY_RESET", "月额度重置");

    private final String value;
    private final String text;

    CreditBizTypeEnum(String value, String text) {
        this.value = value;
        this.text = text;
    }
}
