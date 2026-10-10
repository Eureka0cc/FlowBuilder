package com.eureka.flowbuilder.controller;

import com.eureka.flowbuilder.annotation.AuthCheck;
import com.eureka.flowbuilder.common.BaseResponse;
import com.eureka.flowbuilder.common.ResultUtils;
import com.eureka.flowbuilder.constant.UserConstant;
import com.eureka.flowbuilder.exception.ErrorCode;
import com.eureka.flowbuilder.exception.ThrowUtils;
import com.eureka.flowbuilder.model.dto.billing.AdminAssignPlanRequest;
import com.eureka.flowbuilder.model.dto.billing.AdminCreditAdjustRequest;
import com.eureka.flowbuilder.model.dto.billing.AdminCreditRechargeRequest;
import com.eureka.flowbuilder.model.entity.CreditLedger;
import com.eureka.flowbuilder.model.entity.User;
import com.eureka.flowbuilder.model.vo.billing.BillingAccountVO;
import com.eureka.flowbuilder.model.vo.billing.CreditLedgerVO;
import com.eureka.flowbuilder.config.BillingProperties;
import com.eureka.flowbuilder.service.CreditService;
import com.eureka.flowbuilder.service.UserService;
import com.mybatisflex.core.paginate.Page;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.math.RoundingMode;

@RestController
@RequestMapping("/admin/billing")
public class AdminBillingController {

    @Resource
    private UserService userService;

    @Resource
    private CreditService creditService;

    @Resource
    private BillingProperties billingProperties;

    @PostMapping("/recharge")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<BigDecimal> recharge(@RequestBody AdminCreditRechargeRequest request,
                                             HttpServletRequest httpRequest) {
        ThrowUtils.throwIf(request == null || request.getUserId() == null, ErrorCode.PARAMS_ERROR);
        User admin = userService.getLoginUser(httpRequest);
        BigDecimal balance = creditService.recharge(
                request.getUserId(), request.getAmount(), request.getRemark(), admin.getId());
        return ResultUtils.success(toDisplay(balance));
    }

    @PostMapping("/adjust")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<BigDecimal> adjust(@RequestBody AdminCreditAdjustRequest request,
                                           HttpServletRequest httpRequest) {
        ThrowUtils.throwIf(request == null || request.getUserId() == null, ErrorCode.PARAMS_ERROR);
        User admin = userService.getLoginUser(httpRequest);
        BigDecimal balance = creditService.adjust(
                request.getUserId(), request.getAmount(), request.getRemark(), admin.getId());
        return ResultUtils.success(toDisplay(balance));
    }

    @PostMapping("/plan/assign")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> assignPlan(@RequestBody AdminAssignPlanRequest request,
                                            HttpServletRequest httpRequest) {
        ThrowUtils.throwIf(request == null || request.getUserId() == null, ErrorCode.PARAMS_ERROR);
        User admin = userService.getLoginUser(httpRequest);
        boolean reset = request.getResetMonthlyQuota() == null || request.getResetMonthlyQuota();
        creditService.assignPlan(request.getUserId(), request.getPlanCode(),
                request.getPlanExpireTime(), reset, admin.getId());
        return ResultUtils.success(true);
    }

    @GetMapping("/account")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<BillingAccountVO> getUserAccount(@RequestParam Long userId) {
        ThrowUtils.throwIf(userId == null, ErrorCode.PARAMS_ERROR);
        User user = userService.getById(userId);
        ThrowUtils.throwIf(user == null, ErrorCode.NOT_FOUND_ERROR, "用户不存在");
        return ResultUtils.success(creditService.getAccount(user));
    }

    @GetMapping("/ledger/page")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Page<CreditLedgerVO>> pageLedger(
            @RequestParam Long userId,
            @RequestParam(defaultValue = "1") long pageNum,
            @RequestParam(defaultValue = "10") long pageSize) {
        ThrowUtils.throwIf(userId == null || pageNum <= 0 || pageSize <= 0 || pageSize > 100, ErrorCode.PARAMS_ERROR);
        Page<CreditLedger> page = creditService.pageLedger(userId, pageNum, pageSize);
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
        return (cny == null ? BigDecimal.ZERO : cny).multiply(factor).setScale(2, RoundingMode.HALF_UP);
    }
}
