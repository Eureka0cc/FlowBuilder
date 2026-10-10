package com.eureka.flowbuilder.model.vo.admin;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public final class AdminStatsVO {

    private AdminStatsVO() {
    }

    public record Overview(
            long totalUsers,
            long todayNewUsers,
            long totalApps,
            long todayNewApps,
            long todayConversations,
            BigDecimal deploymentRate,
            long totalTokens,
            long todayTokens,
            BigDecimal totalEstimatedCost,
            BigDecimal todayEstimatedCost,
            String currency
    ) {
    }

    public record TrendPoint(
            LocalDate statDate,
            long newUsers,
            long newApps,
            long conversations,
            long inputTokens,
            long outputTokens,
            BigDecimal estimatedCost
    ) {
    }

    public record Distribution(List<NameValue> codeGenTypes, List<NameValue> modelUsages) {
    }

    public record NameValue(String name, long value) {
    }

    public record TopUser(
            Long userId,
            String userName,
            String userAvatar,
            long totalTokens,
            BigDecimal estimatedCost,
            long callCount
    ) {
    }

    public record UsageRecord(
            Long id,
            Long userId,
            Long appId,
            String modelName,
            String codeGenType,
            String invocationType,
            int inputTokens,
            int outputTokens,
            int totalTokens,
            BigDecimal estimatedCost,
            String currency,
            String status,
            Integer durationMs,
            LocalDateTime createTime
    ) {
    }

    public record RecentPage(List<UsageRecord> records, long totalRow) {
    }
}
