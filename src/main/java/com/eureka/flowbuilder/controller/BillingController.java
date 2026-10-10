package com.eureka.flowbuilder.controller;

import com.eureka.flowbuilder.common.BaseResponse;
import com.eureka.flowbuilder.common.ResultUtils;
import com.eureka.flowbuilder.config.BillingProperties;
import com.eureka.flowbuilder.exception.ErrorCode;
import com.eureka.flowbuilder.exception.ThrowUtils;
import com.eureka.flowbuilder.mapper.AppMapper;
import com.eureka.flowbuilder.model.entity.CreditLedger;
import com.eureka.flowbuilder.model.entity.User;
import com.eureka.flowbuilder.model.vo.billing.BillingAccountVO;
import com.eureka.flowbuilder.model.vo.billing.CreditLedgerVO;
import com.eureka.flowbuilder.service.CreditService;
import com.eureka.flowbuilder.service.UserService;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@RestController
@RequestMapping("/billing")
public class BillingController {

    @Resource
    private UserService userService;

    @Resource
    private CreditService creditService;

    @Resource
    private AppMapper appMapper;

    @Resource
    private BillingProperties billingProperties;

    @GetMapping("/account")
    public BaseResponse<BillingAccountVO> getAccount(HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        BillingAccountVO account = creditService.getAccount(loginUser);
        long appCount = appMapper.selectCountByQuery(QueryWrapper.create().eq("userId", loginUser.getId()));
        account.setAppCount((int) appCount);
        return ResultUtils.success(account);
    }

    @GetMapping("/plans")
    public BaseResponse<List<BillingAccountVO.PlanOptionVO>> listPlans() {
        BillingAccountVO account = BillingAccountVO.builder()
                .plans(billingProperties.getPlans().entrySet().stream().map(entry -> {
                    BillingProperties.PlanConfig cfg = entry.getValue();
                    return BillingAccountVO.PlanOptionVO.builder()
                            .planCode(entry.getKey())
                            .name(cfg.getName())
                            .description(cfg.getDescription())
                            .monthlyQuota(nz(cfg.getMonthlyQuota()))
                            .monthlyQuotaDisplay(toDisplay(cfg.getMonthlyQuota()))
                            .appLimit(cfg.getAppLimit())
                            .chatRate(cfg.getChatRate())
                            .signupGift(nz(cfg.getSignupGift()))
                            .signupGiftDisplay(toDisplay(cfg.getSignupGift()))
                            .build();
                }).toList())
                .build();
        return ResultUtils.success(account.getPlans());
    }

    @GetMapping("/ledger/page")
    public BaseResponse<Page<CreditLedgerVO>> pageLedger(
            @RequestParam(defaultValue = "1") long pageNum,
            @RequestParam(defaultValue = "10") long pageSize,
            HttpServletRequest request) {
        ThrowUtils.throwIf(pageNum <= 0 || pageSize <= 0 || pageSize > 100, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(request);
        Page<CreditLedger> page = creditService.pageLedger(loginUser.getId(), pageNum, pageSize);
        Page<CreditLedgerVO> voPage = new Page<>(page.getPageNumber(), page.getPageSize(), page.getTotalRow());
        voPage.setRecords(page.getRecords().stream().map(this::toVO).toList());
        return ResultUtils.success(voPage);
    }

    private CreditLedgerVO toVO(CreditLedger ledger) {
        return CreditLedgerVO.builder()
                .id(ledger.getId())
                .userId(ledger.getUserId())
                .changeAmount(ledger.getChangeAmount())
                .changeDisplay(toDisplay(ledger.getChangeAmount()))
                .balanceAfter(ledger.getBalanceAfter())
                .balanceAfterDisplay(toDisplay(ledger.getBalanceAfter()))
                .bizType(ledger.getBizType())
                .bizId(ledger.getBizId())
                .remark(ledger.getRemark())
                .operatorId(ledger.getOperatorId())
                .createTime(ledger.getCreateTime())
                .build();
    }

    private BigDecimal toDisplay(BigDecimal cny) {
        BigDecimal factor = billingProperties.getCreditDisplayFactor() == null
                ? new BigDecimal("100")
                : billingProperties.getCreditDisplayFactor();
        return nz(cny).multiply(factor).setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal nz(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
