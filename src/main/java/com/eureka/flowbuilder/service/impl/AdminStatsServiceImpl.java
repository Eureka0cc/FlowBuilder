package com.eureka.flowbuilder.service.impl;

import com.eureka.flowbuilder.config.AiUsageProperties;
import com.eureka.flowbuilder.model.vo.admin.AdminStatsVO;
import com.eureka.flowbuilder.service.AdminStatsService;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class AdminStatsServiceImpl implements AdminStatsService {

    private final JdbcTemplate jdbcTemplate;
    private final AiUsageProperties usageProperties;

    public AdminStatsServiceImpl(JdbcTemplate jdbcTemplate, AiUsageProperties usageProperties) {
        this.jdbcTemplate = jdbcTemplate;
        this.usageProperties = usageProperties;
    }

    @Override
    public AdminStatsVO.Overview getOverview(int days) {
        LocalDateTime today = LocalDate.now().atStartOfDay();
        long totalUsers = count("SELECT COUNT(*) FROM `user` WHERE isDelete = 0");
        long todayUsers = count("SELECT COUNT(*) FROM `user` WHERE isDelete = 0 AND createTime >= ?", today);
        long totalApps = count("SELECT COUNT(*) FROM app WHERE isDelete = 0");
        long todayApps = count("SELECT COUNT(*) FROM app WHERE isDelete = 0 AND createTime >= ?", today);
        long deployedApps = count("SELECT COUNT(*) FROM app WHERE isDelete = 0 AND deployKey IS NOT NULL AND deployKey <> ''");
        long todayConversations = count("""
                SELECT COUNT(*) FROM chat_history
                WHERE isDelete = 0 AND messageType = 'user' AND createTime >= ?
                """, today);
        long totalTokens = count("SELECT COALESCE(SUM(totalTokens), 0) FROM ai_usage_log WHERE status = 'SUCCESS'");
        long todayTokens = count("""
                SELECT COALESCE(SUM(totalTokens), 0) FROM ai_usage_log
                WHERE status = 'SUCCESS' AND createTime >= ?
                """, today);
        BigDecimal totalCost = decimal("SELECT SUM(estimatedCost) FROM ai_usage_log WHERE status = 'SUCCESS'");
        BigDecimal todayCost = decimal("""
                SELECT SUM(estimatedCost) FROM ai_usage_log
                WHERE status = 'SUCCESS' AND createTime >= ?
                """, today);
        BigDecimal deploymentRate = totalApps == 0
                ? BigDecimal.ZERO
                : BigDecimal.valueOf(deployedApps * 100L)
                .divide(BigDecimal.valueOf(totalApps), 2, RoundingMode.HALF_UP);

        return new AdminStatsVO.Overview(totalUsers, todayUsers, totalApps, todayApps,
                todayConversations, deploymentRate, totalTokens, todayTokens,
                totalCost, todayCost, usageProperties.getCurrency());
    }

    @Override
    public List<AdminStatsVO.TrendPoint> getTrend(int days) {
        LocalDate startDate = LocalDate.now().minusDays(days - 1L);
        Map<LocalDate, MutableTrend> trends = new LinkedHashMap<>();
        for (int i = 0; i < days; i++) {
            trends.put(startDate.plusDays(i), new MutableTrend());
        }
        fillCountTrend("SELECT DATE(createTime) d, COUNT(*) v FROM `user` WHERE isDelete = 0 AND createTime >= ? GROUP BY DATE(createTime)",
                startDate, trends, TrendField.USERS);
        fillCountTrend("SELECT DATE(createTime) d, COUNT(*) v FROM app WHERE isDelete = 0 AND createTime >= ? GROUP BY DATE(createTime)",
                startDate, trends, TrendField.APPS);
        fillCountTrend("""
                SELECT DATE(createTime) d, COUNT(*) v FROM chat_history
                WHERE isDelete = 0 AND messageType = 'user' AND createTime >= ?
                GROUP BY DATE(createTime)
                """, startDate, trends, TrendField.CONVERSATIONS);
        jdbcTemplate.query("""
                        SELECT DATE(createTime) d,
                               COALESCE(SUM(inputTokens), 0) inputTokens,
                               COALESCE(SUM(outputTokens), 0) outputTokens,
                               COALESCE(SUM(estimatedCost), 0) estimatedCost
                        FROM ai_usage_log
                        WHERE status = 'SUCCESS' AND createTime >= ?
                        GROUP BY DATE(createTime)
                        """,
                rs -> {
                    MutableTrend trend = trends.get(rs.getDate("d").toLocalDate());
                    if (trend != null) {
                        trend.inputTokens = rs.getLong("inputTokens");
                        trend.outputTokens = rs.getLong("outputTokens");
                        trend.estimatedCost = rs.getBigDecimal("estimatedCost");
                    }
                }, Timestamp.valueOf(startDate.atStartOfDay()));
        return trends.entrySet().stream()
                .map(entry -> new AdminStatsVO.TrendPoint(entry.getKey(), entry.getValue().users,
                        entry.getValue().apps, entry.getValue().conversations,
                        entry.getValue().inputTokens, entry.getValue().outputTokens,
                        entry.getValue().estimatedCost))
                .toList();
    }

    @Override
    public AdminStatsVO.Distribution getDistribution() {
        List<AdminStatsVO.NameValue> codeGenTypes = jdbcTemplate.query("""
                SELECT COALESCE(codeGenType, 'unknown') name, COUNT(*) value
                FROM app WHERE isDelete = 0 GROUP BY codeGenType ORDER BY value DESC
                """, (rs, rowNum) -> new AdminStatsVO.NameValue(rs.getString("name"), rs.getLong("value")));
        List<AdminStatsVO.NameValue> modelUsages = jdbcTemplate.query("""
                SELECT modelName name, COALESCE(SUM(totalTokens), 0) value
                FROM ai_usage_log WHERE status = 'SUCCESS'
                GROUP BY modelName ORDER BY value DESC
                """, (rs, rowNum) -> new AdminStatsVO.NameValue(rs.getString("name"), rs.getLong("value")));
        return new AdminStatsVO.Distribution(codeGenTypes, modelUsages);
    }

    @Override
    public List<AdminStatsVO.TopUser> getTopUsers(int days, int limit) {
        LocalDateTime start = LocalDate.now().minusDays(days - 1L).atStartOfDay();
        return jdbcTemplate.query("""
                        SELECT a.userId,
                               COALESCE(u.userName, u.userAccount, CONCAT('用户 ', a.userId)) userName,
                               u.userAvatar,
                               COALESCE(SUM(a.totalTokens), 0) totalTokens,
                               COALESCE(SUM(a.estimatedCost), 0) estimatedCost,
                               COUNT(*) callCount
                        FROM ai_usage_log a
                        LEFT JOIN `user` u ON u.id = a.userId AND u.isDelete = 0
                        WHERE a.status = 'SUCCESS' AND a.createTime >= ? AND a.userId IS NOT NULL
                        GROUP BY a.userId, u.userName, u.userAccount, u.userAvatar
                        ORDER BY totalTokens DESC LIMIT ?
                        """,
                (rs, rowNum) -> new AdminStatsVO.TopUser(
                        rs.getLong("userId"), rs.getString("userName"), rs.getString("userAvatar"),
                        rs.getLong("totalTokens"), rs.getBigDecimal("estimatedCost"), rs.getLong("callCount")),
                start, limit);
    }

    @Override
    public AdminStatsVO.RecentPage getRecent(int pageNum, int pageSize) {
        long total = count("SELECT COUNT(*) FROM ai_usage_log");
        int offset = (pageNum - 1) * pageSize;
        List<AdminStatsVO.UsageRecord> records = jdbcTemplate.query("""
                        SELECT id, userId, appId, modelName, codeGenType, invocationType,
                               inputTokens, outputTokens, totalTokens, estimatedCost,
                               currency, status, durationMs, createTime
                        FROM ai_usage_log ORDER BY createTime DESC LIMIT ? OFFSET ?
                        """,
                (rs, rowNum) -> new AdminStatsVO.UsageRecord(
                        rs.getLong("id"), nullableLong(rs, "userId"), nullableLong(rs, "appId"),
                        rs.getString("modelName"), rs.getString("codeGenType"),
                        rs.getString("invocationType"), rs.getInt("inputTokens"),
                        rs.getInt("outputTokens"), rs.getInt("totalTokens"),
                        rs.getBigDecimal("estimatedCost"), rs.getString("currency"),
                        rs.getString("status"), (Integer) rs.getObject("durationMs"),
                        rs.getTimestamp("createTime").toLocalDateTime()),
                pageSize, offset);
        return new AdminStatsVO.RecentPage(records, total);
    }

    private void fillCountTrend(String sql, LocalDate startDate, Map<LocalDate, MutableTrend> trends,
                                TrendField field) {
        jdbcTemplate.query(sql, rs -> {
            MutableTrend trend = trends.get(rs.getDate("d").toLocalDate());
            if (trend == null) {
                return;
            }
            long value = rs.getLong("v");
            switch (field) {
                case USERS -> trend.users = value;
                case APPS -> trend.apps = value;
                case CONVERSATIONS -> trend.conversations = value;
            }
        }, Timestamp.valueOf(startDate.atStartOfDay()));
    }

    private long count(String sql, Object... args) {
        Long result = jdbcTemplate.queryForObject(sql, Long.class, args);
        return result == null ? 0 : result;
    }

    private BigDecimal decimal(String sql, Object... args) {
        return jdbcTemplate.queryForObject(sql, BigDecimal.class, args);
    }

    private Long nullableLong(java.sql.ResultSet rs, String column) throws java.sql.SQLException {
        long value = rs.getLong(column);
        return rs.wasNull() ? null : value;
    }

    private enum TrendField {USERS, APPS, CONVERSATIONS}

    private static final class MutableTrend {
        private long users;
        private long apps;
        private long conversations;
        private long inputTokens;
        private long outputTokens;
        private BigDecimal estimatedCost = BigDecimal.ZERO;
    }
}
