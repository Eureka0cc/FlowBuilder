package com.eureka.flowbuilder.service.impl;

import com.eureka.flowbuilder.config.AiUsageProperties;
import com.eureka.flowbuilder.mapper.AiUsageLogMapper;
import com.eureka.flowbuilder.model.entity.AiUsageLog;
import com.eureka.flowbuilder.service.AiUsageLogService;
import com.eureka.flowbuilder.service.CreditService;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import dev.langchain4j.model.output.TokenUsage;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

@Slf4j
@Service
public class AiUsageLogServiceImpl extends ServiceImpl<AiUsageLogMapper, AiUsageLog>
        implements AiUsageLogService {

    private static final BigDecimal ONE_MILLION = BigDecimal.valueOf(1_000_000);
    private final AiUsageProperties properties;

    @Lazy
    @Resource
    private CreditService creditService;

    public AiUsageLogServiceImpl(AiUsageProperties properties) {
        this.properties = properties;
    }

    @Override
    public void recordSuccess(Long appId, Long userId, String modelName, String codeGenType,
                              String invocationType, TokenUsage tokenUsage, long durationMs) {
        int inputTokens = tokenUsage == null ? 0 : tokenUsage.inputTokenCount();
        int outputTokens = tokenUsage == null ? 0 : tokenUsage.outputTokenCount();
        int totalTokens = tokenUsage == null ? inputTokens + outputTokens : tokenUsage.totalTokenCount();
        AiUsageProperties.ModelPrice price = properties.getModels().get(modelName);
        if (price != null && (price.getInputPricePerMillion() == null
                || price.getOutputPricePerMillion() == null
                || (price.getInputPricePerMillion().signum() == 0
                && price.getOutputPricePerMillion().signum() == 0))) {
            price = null;
        }
        BigDecimal inputPrice = price == null ? null : price.getInputPricePerMillion();
        BigDecimal outputPrice = price == null ? null : price.getOutputPricePerMillion();
        BigDecimal cost = calculateCost(inputTokens, outputTokens, inputPrice, outputPrice);

        persistAsync(AiUsageLog.builder()
                .appId(appId)
                .userId(userId)
                .modelName(modelName)
                .codeGenType(codeGenType)
                .invocationType(invocationType)
                .inputTokens(inputTokens)
                .outputTokens(outputTokens)
                .totalTokens(totalTokens)
                .inputUnitPrice(inputPrice)
                .outputUnitPrice(outputPrice)
                .estimatedCost(cost)
                .currency(price == null ? null : properties.getCurrency())
                .status("SUCCESS")
                .durationMs(toDuration(durationMs))
                .createTime(LocalDateTime.now())
                .build(), true);
    }

    @Override
    public void recordFailure(Long appId, Long userId, String modelName, String codeGenType,
                              String invocationType, long durationMs, Throwable error) {
        String message = error == null ? null : error.getMessage();
        if (message != null && message.length() > 500) {
            message = message.substring(0, 500);
        }
        persistAsync(AiUsageLog.builder()
                .appId(appId)
                .userId(userId)
                .modelName(modelName)
                .codeGenType(codeGenType)
                .invocationType(invocationType)
                .inputTokens(0)
                .outputTokens(0)
                .totalTokens(0)
                .status("FAILED")
                .durationMs(toDuration(durationMs))
                .errorMessage(message)
                .createTime(LocalDateTime.now())
                .build(), false);
    }

    private BigDecimal calculateCost(int inputTokens, int outputTokens,
                                     BigDecimal inputPrice, BigDecimal outputPrice) {
        if (inputPrice == null || outputPrice == null) {
            return null;
        }
        BigDecimal inputCost = inputPrice.multiply(BigDecimal.valueOf(inputTokens)).divide(ONE_MILLION, 8, RoundingMode.HALF_UP);
        BigDecimal outputCost = outputPrice.multiply(BigDecimal.valueOf(outputTokens)).divide(ONE_MILLION, 8, RoundingMode.HALF_UP);
        return inputCost.add(outputCost);
    }

    private void persistAsync(AiUsageLog usageLog, boolean deductCredit) {
        Thread.startVirtualThread(() -> {
            try {
                save(usageLog);
                if (deductCredit && usageLog.getUserId() != null && usageLog.getId() != null) {
                    creditService.consumeForUsage(
                            usageLog.getUserId(),
                            usageLog.getId(),
                            usageLog.getEstimatedCost(),
                            usageLog.getTotalTokens());
                }
            } catch (Exception e) {
                log.error("AI 用量记录失败: appId={}, model={}", usageLog.getAppId(), usageLog.getModelName(), e);
            }
        });
    }

    private int toDuration(long durationMs) {
        return (int) Math.min(Math.max(durationMs, 0), Integer.MAX_VALUE);
    }
}
