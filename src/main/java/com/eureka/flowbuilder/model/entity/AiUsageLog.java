package com.eureka.flowbuilder.model.entity;

import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import com.mybatisflex.annotation.Column;
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
@Table("ai_usage_log")
public class AiUsageLog {

    @Id(keyType = KeyType.Generator, value = KeyGenerators.snowFlakeId)
    private Long id;

    @Column("userId")
    private Long userId;
    @Column("appId")
    private Long appId;
    @Column("modelName")
    private String modelName;
    @Column("codeGenType")
    private String codeGenType;
    @Column("invocationType")
    private String invocationType;
    @Column("inputTokens")
    private Integer inputTokens;
    @Column("outputTokens")
    private Integer outputTokens;
    @Column("totalTokens")
    private Integer totalTokens;
    @Column("inputUnitPrice")
    private BigDecimal inputUnitPrice;
    @Column("outputUnitPrice")
    private BigDecimal outputUnitPrice;
    @Column("estimatedCost")
    private BigDecimal estimatedCost;
    private String currency;
    private String status;
    @Column("durationMs")
    private Integer durationMs;
    @Column("errorMessage")
    private String errorMessage;
    @Column("createTime")
    private LocalDateTime createTime;
}
