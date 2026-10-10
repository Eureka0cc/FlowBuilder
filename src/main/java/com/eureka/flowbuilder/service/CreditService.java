package com.eureka.flowbuilder.service;

import com.eureka.flowbuilder.config.BillingProperties;
import com.eureka.flowbuilder.model.entity.CreditLedger;
import com.eureka.flowbuilder.model.entity.User;
import com.eureka.flowbuilder.model.vo.billing.BillingAccountVO;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.service.IService;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public interface CreditService extends IService<CreditLedger> {

    void initForNewUser(Long userId);

    void ensureSufficient(Long userId);

    void ensureAppLimit(Long userId, long currentAppCount);

    void tryMonthlyReset(Long userId);

    void consumeForUsage(Long userId, Long usageLogId, BigDecimal estimatedCost, Integer totalTokens);

    BigDecimal recharge(Long userId, BigDecimal amount, String remark, Long operatorId);

    BigDecimal adjust(Long userId, BigDecimal amount, String remark, Long operatorId);

    void assignPlan(Long userId, String planCode, LocalDateTime planExpireTime,
                    boolean resetMonthlyQuota, Long operatorId);

    BillingAccountVO getAccount(User user);

    Page<CreditLedger> pageLedger(Long userId, long pageNum, long pageSize);

    BillingProperties.PlanConfig resolvePlan(String planCode);
}
