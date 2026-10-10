package com.eureka.flowbuilder.model.entity;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import com.mybatisflex.core.keygen.KeyGenerators;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("credit_ledger")
public class CreditLedger {

    @Id(keyType = KeyType.Generator, value = KeyGenerators.snowFlakeId)
    private Long id;

    @Column("userId")
    private Long userId;

    @Column("changeAmount")
    private BigDecimal changeAmount;

    @Column("balanceAfter")
    private BigDecimal balanceAfter;

    @Column("bizType")
    private String bizType;

    @Column("bizId")
    private String bizId;

    private String remark;

    @Column("operatorId")
    private Long operatorId;

    @Column("createTime")
    private LocalDateTime createTime;
}
