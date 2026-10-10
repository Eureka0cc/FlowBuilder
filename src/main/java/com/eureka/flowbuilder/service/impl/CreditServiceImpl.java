package com.eureka.flowbuilder.service.impl;

import cn.hutool.core.util.StrUtil;
import com.eureka.flowbuilder.config.BillingProperties;
import com.eureka.flowbuilder.exception.BusinessException;
import com.eureka.flowbuilder.exception.ErrorCode;
import com.eureka.flowbuilder.exception.ThrowUtils;
import com.eureka.flowbuilder.mapper.CreditLedgerMapper;
import com.eureka.flowbuilder.mapper.UserMapper;
import com.eureka.flowbuilder.model.entity.CreditLedger;
import com.eureka.flowbuilder.model.entity.User;
import com.eureka.flowbuilder.model.enums.CreditBizTypeEnum;
import com.eureka.flowbuilder.model.vo.billing.BillingAccountVO;
import com.eureka.flowbuilder.service.CreditService;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.update.UpdateChain;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
public class CreditServiceImpl extends ServiceImpl<CreditLedgerMapper, CreditLedger> implements CreditService {

    @Resource
    private UserMapper userMapper;

    @Resource
    private BillingProperties billingProperties;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void initForNewUser(Long userId) {
        ThrowUtils.throwIf(userId == null, ErrorCode.PARAMS_ERROR);
        User user = userMapper.selectOneById(userId);
        ThrowUtils.throwIf(user == null, ErrorCode.NOT_FOUND_ERROR, "用户不存在");
        BillingProperties.PlanConfig freePlan = resolvePlan("free");
        BigDecimal gift = nullToZero(freePlan.getSignupGift());
        LocalDateTime now = LocalDateTime.now();
        UpdateChain.of(User.class)
                .set(User::getPlanCode, "free")
                .set(User::getCreditBalance, gift)
                .set(User::getCreditUpdatedTime, now)
                .set(User::getMonthlyQuotaResetAt, now)
                .where(User::getId).eq(userId)
                .update();
        if (gift.signum() > 0) {
            writeLedger(userId, gift, gift, CreditBizTypeEnum.GIFT,
                    "signup-" + userId, "新用户注册赠送", null);
        }
    }

    @Override
    public void ensureSufficient(Long userId) {
        tryMonthlyReset(userId);
        User user = requireUser(userId);
        BigDecimal balance = nullToZero(user.getCreditBalance());
        BigDecimal minReserve = nullToZero(billingProperties.getMinReserve());
        if (balance.compareTo(minReserve) < 0) {
            throw new BusinessException(ErrorCode.CREDIT_INSUFFICIENT,
                    "积分余额不足，请联系管理员充值或升级套餐");
        }
    }

    @Override
    public void ensureAppLimit(Long userId, long currentAppCount) {
        User user = requireUser(userId);
        BillingProperties.PlanConfig plan = resolveEffectivePlan(user);
        Integer appLimit = plan.getAppLimit();
        if (appLimit != null && currentAppCount >= appLimit) {
            throw new BusinessException(ErrorCode.PLAN_LIMIT_EXCEEDED,
                    "当前套餐最多可创建 " + appLimit + " 个应用，请升级套餐");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void tryMonthlyReset(Long userId) {
        User user = requireUser(userId);
        LocalDateTime resetAt = user.getMonthlyQuotaResetAt();
        YearMonth currentMonth = YearMonth.now();
        if (resetAt != null && YearMonth.from(resetAt).equals(currentMonth)) {
            return;
        }
        BillingProperties.PlanConfig plan = resolveEffectivePlan(user);
        BigDecimal monthlyQuota = nullToZero(plan.getMonthlyQuota());
        if (monthlyQuota.signum() <= 0) {
            UpdateChain.of(User.class)
                    .set(User::getMonthlyQuotaResetAt, LocalDateTime.now())
                    .where(User::getId).eq(userId)
                    .update();
            return;
        }
        BigDecimal balance = nullToZero(user.getCreditBalance());
        if (balance.compareTo(monthlyQuota) >= 0) {
            UpdateChain.of(User.class)
                    .set(User::getMonthlyQuotaResetAt, LocalDateTime.now())
                    .where(User::getId).eq(userId)
                    .update();
            return;
        }
        BigDecimal delta = monthlyQuota.subtract(balance);
        LocalDateTime now = LocalDateTime.now();
        boolean updated = UpdateChain.of(User.class)
                .set(User::getCreditBalance, monthlyQuota)
                .set(User::getCreditUpdatedTime, now)
                .set(User::getMonthlyQuotaResetAt, now)
                .where(User::getId).eq(userId)
                .and(User::getCreditBalance).eq(balance)
                .update();
        if (updated) {
            writeLedger(userId, delta, monthlyQuota, CreditBizTypeEnum.MONTHLY_RESET,
                    "monthly-" + userId + "-" + currentMonth, "月额度重置至套餐配额", null);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void consumeForUsage(Long userId, Long usageLogId, BigDecimal estimatedCost, Integer totalTokens) {
        if (userId == null || usageLogId == null) {
            return;
        }
        String bizId = String.valueOf(usageLogId);
        long exists = this.count(QueryWrapper.create()
                .eq("bizType", CreditBizTypeEnum.CONSUME.getValue())
                .eq("bizId", bizId));
        if (exists > 0) {
            return;
        }
        BigDecimal amount = resolveConsumeAmount(estimatedCost, totalTokens);
        if (amount.signum() <= 0) {
            return;
        }
        for (int i = 0; i < 3; i++) {
            User user = requireUser(userId);
            BigDecimal balance = nullToZero(user.getCreditBalance());
            BigDecimal next = balance.subtract(amount);
            if (next.signum() < 0) {
                next = BigDecimal.ZERO.setScale(8, RoundingMode.HALF_UP);
                amount = balance;
            }
            if (amount.signum() <= 0) {
                return;
            }
            LocalDateTime now = LocalDateTime.now();
            boolean updated = UpdateChain.of(User.class)
                    .set(User::getCreditBalance, next)
                    .set(User::getCreditUpdatedTime, now)
                    .where(User::getId).eq(userId)
                    .and(User::getCreditBalance).eq(balance)
                    .update();
            if (updated) {
                try {
                    writeLedger(userId, amount.negate(), next, CreditBizTypeEnum.CONSUME,
                            bizId, "AI 调用消耗", null);
                } catch (DataIntegrityViolationException e) {
                    log.warn("重复扣减已忽略 usageLogId={}", usageLogId);
                }
                return;
            }
        }
        log.error("积分扣减失败（并发冲突） userId={}, usageLogId={}", userId, usageLogId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BigDecimal recharge(Long userId, BigDecimal amount, String remark, Long operatorId) {
        ThrowUtils.throwIf(amount == null || amount.signum() <= 0, ErrorCode.PARAMS_ERROR, "充值金额必须为正数");
        return changeBalance(userId, amount, CreditBizTypeEnum.RECHARGE,
                "recharge-" + UUID.randomUUID(),
                StrUtil.blankToDefault(remark, "管理员充值"), operatorId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BigDecimal adjust(Long userId, BigDecimal amount, String remark, Long operatorId) {
        ThrowUtils.throwIf(amount == null || amount.signum() == 0, ErrorCode.PARAMS_ERROR, "调账金额不能为 0");
        return changeBalance(userId, amount, CreditBizTypeEnum.ADJUST,
                "adjust-" + UUID.randomUUID(),
                StrUtil.blankToDefault(remark, "管理员调账"), operatorId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void assignPlan(Long userId, String planCode, LocalDateTime planExpireTime,
                           boolean resetMonthlyQuota, Long operatorId) {
        ThrowUtils.throwIf(StrUtil.isBlank(planCode), ErrorCode.PARAMS_ERROR, "套餐编码不能为空");
        BillingProperties.PlanConfig plan = billingProperties.getPlans().get(planCode);
        ThrowUtils.throwIf(plan == null, ErrorCode.PARAMS_ERROR, "不支持的套餐: " + planCode);
        User user = requireUser(userId);
        LocalDateTime now = LocalDateTime.now();
        UpdateChain.of(User.class)
                .set(User::getPlanCode, planCode)
                .set(User::getPlanExpireTime, planExpireTime)
                .where(User::getId).eq(userId)
                .update();
        if (resetMonthlyQuota) {
            BigDecimal monthlyQuota = nullToZero(plan.getMonthlyQuota());
            BigDecimal balance = nullToZero(user.getCreditBalance());
            if (monthlyQuota.compareTo(balance) > 0) {
                BigDecimal delta = monthlyQuota.subtract(balance);
                UpdateChain.of(User.class)
                        .set(User::getCreditBalance, monthlyQuota)
                        .set(User::getCreditUpdatedTime, now)
                        .set(User::getMonthlyQuotaResetAt, now)
                        .where(User::getId).eq(userId)
                        .update();
                writeLedger(userId, delta, monthlyQuota, CreditBizTypeEnum.MONTHLY_RESET,
                        "assign-plan-" + userId + "-" + UUID.randomUUID(),
                        "更换套餐并重置月额度: " + planCode, operatorId);
            } else {
                UpdateChain.of(User.class)
                        .set(User::getMonthlyQuotaResetAt, now)
                        .where(User::getId).eq(userId)
                        .update();
            }
        }
    }

    @Override
    public BillingAccountVO getAccount(User user) {
        ThrowUtils.throwIf(user == null, ErrorCode.NOT_LOGIN_ERROR);
        tryMonthlyReset(user.getId());
        user = requireUser(user.getId());
        BillingProperties.PlanConfig plan = resolveEffectivePlan(user);
        BigDecimal balance = nullToZero(user.getCreditBalance());
        List<BillingAccountVO.PlanOptionVO> plans = new ArrayList<>();
        for (Map.Entry<String, BillingProperties.PlanConfig> entry : billingProperties.getPlans().entrySet()) {
            BillingProperties.PlanConfig cfg = entry.getValue();
            plans.add(BillingAccountVO.PlanOptionVO.builder()
                    .planCode(entry.getKey())
                    .name(cfg.getName())
                    .description(cfg.getDescription())
                    .monthlyQuota(nullToZero(cfg.getMonthlyQuota()))
                    .monthlyQuotaDisplay(toDisplay(cfg.getMonthlyQuota()))
                    .appLimit(cfg.getAppLimit())
                    .chatRate(cfg.getChatRate())
                    .signupGift(nullToZero(cfg.getSignupGift()))
                    .signupGiftDisplay(toDisplay(cfg.getSignupGift()))
                    .build());
        }
        return BillingAccountVO.builder()
                .planCode(StrUtil.blankToDefault(user.getPlanCode(), "free"))
                .planName(plan.getName())
                .planDescription(plan.getDescription())
                .creditBalance(balance)
                .creditDisplay(toDisplay(balance))
                .monthlyQuota(nullToZero(plan.getMonthlyQuota()))
                .appLimit(plan.getAppLimit())
                .chatRate(plan.getChatRate())
                .planExpireTime(user.getPlanExpireTime())
                .monthlyQuotaResetAt(user.getMonthlyQuotaResetAt())
                .creditUpdatedTime(user.getCreditUpdatedTime())
                .plans(plans)
                .build();
    }

    @Override
    public Page<CreditLedger> pageLedger(Long userId, long pageNum, long pageSize) {
        QueryWrapper query = QueryWrapper.create()
                .eq("userId", userId)
                .orderBy("createTime", false);
        return this.page(new Page<>(pageNum, pageSize), query);
    }

    @Override
    public BillingProperties.PlanConfig resolvePlan(String planCode) {
        return billingProperties.getPlan(planCode);
    }

    private BigDecimal changeBalance(Long userId, BigDecimal amount, CreditBizTypeEnum bizType,
                                     String bizId, String remark, Long operatorId) {
        for (int i = 0; i < 3; i++) {
            User user = requireUser(userId);
            BigDecimal balance = nullToZero(user.getCreditBalance());
            BigDecimal next = balance.add(amount);
            if (next.signum() < 0) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "调账后余额不能为负");
            }
            LocalDateTime now = LocalDateTime.now();
            boolean updated = UpdateChain.of(User.class)
                    .set(User::getCreditBalance, next)
                    .set(User::getCreditUpdatedTime, now)
                    .where(User::getId).eq(userId)
                    .and(User::getCreditBalance).eq(balance)
                    .update();
            if (updated) {
                writeLedger(userId, amount, next, bizType, bizId, remark, operatorId);
                return next;
            }
        }
        throw new BusinessException(ErrorCode.OPERATION_ERROR, "余额变更失败，请重试");
    }

    private void writeLedger(Long userId, BigDecimal changeAmount, BigDecimal balanceAfter,
                             CreditBizTypeEnum bizType, String bizId, String remark, Long operatorId) {
        CreditLedger ledger = CreditLedger.builder()
                .userId(userId)
                .changeAmount(changeAmount)
                .balanceAfter(balanceAfter)
                .bizType(bizType.getValue())
                .bizId(bizId)
                .remark(remark)
                .operatorId(operatorId)
                .createTime(LocalDateTime.now())
                .build();
        boolean saved = this.save(ledger);
        ThrowUtils.throwIf(!saved, ErrorCode.OPERATION_ERROR, "积分流水写入失败");
    }

    private BigDecimal resolveConsumeAmount(BigDecimal estimatedCost, Integer totalTokens) {
        if (estimatedCost != null && estimatedCost.signum() > 0) {
            return estimatedCost.setScale(8, RoundingMode.HALF_UP);
        }
        BigDecimal minConsume = nullToZero(billingProperties.getMinConsume());
        if (totalTokens != null && totalTokens > 0 && minConsume.signum() > 0) {
            return minConsume;
        }
        return minConsume;
    }

    private BillingProperties.PlanConfig resolveEffectivePlan(User user) {
        String planCode = StrUtil.blankToDefault(user.getPlanCode(), "free");
        LocalDateTime expireTime = user.getPlanExpireTime();
        if (expireTime != null && expireTime.isBefore(LocalDateTime.now()) && !"free".equals(planCode)) {
            UpdateChain.of(User.class)
                    .set(User::getPlanCode, "free")
                    .set(User::getPlanExpireTime, null)
                    .where(User::getId).eq(user.getId())
                    .update();
            return resolvePlan("free");
        }
        return resolvePlan(planCode);
    }

    private User requireUser(Long userId) {
        ThrowUtils.throwIf(userId == null, ErrorCode.PARAMS_ERROR);
        User user = userMapper.selectOneById(userId);
        ThrowUtils.throwIf(user == null, ErrorCode.NOT_FOUND_ERROR, "用户不存在");
        if (user.getCreditBalance() == null) {
            user.setCreditBalance(BigDecimal.ZERO);
        }
        if (StrUtil.isBlank(user.getPlanCode())) {
            user.setPlanCode("free");
        }
        return user;
    }

    private BigDecimal toDisplay(BigDecimal cnyAmount) {
        BigDecimal factor = billingProperties.getCreditDisplayFactor() == null
                ? new BigDecimal("100")
                : billingProperties.getCreditDisplayFactor();
        return nullToZero(cnyAmount).multiply(factor).setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal nullToZero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO.setScale(8, RoundingMode.HALF_UP)
                : value.setScale(8, RoundingMode.HALF_UP);
    }
}
