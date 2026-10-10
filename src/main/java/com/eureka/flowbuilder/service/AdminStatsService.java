package com.eureka.flowbuilder.service;

import com.eureka.flowbuilder.model.vo.admin.AdminStatsVO;

import java.util.List;

public interface AdminStatsService {

    AdminStatsVO.Overview getOverview(int days);

    List<AdminStatsVO.TrendPoint> getTrend(int days);

    AdminStatsVO.Distribution getDistribution();

    List<AdminStatsVO.TopUser> getTopUsers(int days, int limit);

    AdminStatsVO.RecentPage getRecent(int pageNum, int pageSize);
}
