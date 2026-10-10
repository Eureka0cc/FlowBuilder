package com.eureka.flowbuilder.controller;

import com.eureka.flowbuilder.annotation.AuthCheck;
import com.eureka.flowbuilder.common.BaseResponse;
import com.eureka.flowbuilder.common.ResultUtils;
import com.eureka.flowbuilder.constant.UserConstant;
import com.eureka.flowbuilder.exception.ErrorCode;
import com.eureka.flowbuilder.exception.ThrowUtils;
import com.eureka.flowbuilder.model.vo.admin.AdminStatsVO;
import com.eureka.flowbuilder.service.AdminStatsService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/admin/stats")
public class AdminStatsController {

    private final AdminStatsService adminStatsService;

    public AdminStatsController(AdminStatsService adminStatsService) {
        this.adminStatsService = adminStatsService;
    }

    @GetMapping("/overview")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<AdminStatsVO.Overview> overview(
            @RequestParam(defaultValue = "30") int days) {
        validateDays(days);
        return ResultUtils.success(adminStatsService.getOverview(days));
    }

    @GetMapping("/trend")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<List<AdminStatsVO.TrendPoint>> trend(
            @RequestParam(defaultValue = "30") int days) {
        validateDays(days);
        return ResultUtils.success(adminStatsService.getTrend(days));
    }

    @GetMapping("/distribution")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<AdminStatsVO.Distribution> distribution() {
        return ResultUtils.success(adminStatsService.getDistribution());
    }

    @GetMapping("/top-users")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<List<AdminStatsVO.TopUser>> topUsers(
            @RequestParam(defaultValue = "30") int days,
            @RequestParam(defaultValue = "10") int limit) {
        validateDays(days);
        ThrowUtils.throwIf(limit < 1 || limit > 50, ErrorCode.PARAMS_ERROR, "排行数量必须在 1-50 之间");
        return ResultUtils.success(adminStatsService.getTopUsers(days, limit));
    }

    @GetMapping("/recent")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<AdminStatsVO.RecentPage> recent(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "8") int pageSize) {
        ThrowUtils.throwIf(pageNum < 1, ErrorCode.PARAMS_ERROR, "页码必须大于 0");
        ThrowUtils.throwIf(pageSize < 1 || pageSize > 50, ErrorCode.PARAMS_ERROR, "页面大小必须在 1-50 之间");
        return ResultUtils.success(adminStatsService.getRecent(pageNum, pageSize));
    }

    private void validateDays(int days) {
        ThrowUtils.throwIf(days < 1 || days > 90, ErrorCode.PARAMS_ERROR, "统计周期必须在 1-90 天之间");
    }
}
