package com.business.erp.performance.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class ManagementPerformanceDashboardResponse {
    private Integer periodYear;
    private Integer periodMonth;
    private List<PerformanceRankingEntry> topPerformers;
    private List<PerformanceRankingEntry> lowestPerformers;
    private List<PerformanceRankingEntry> targetAchievementRanking;
    private List<PerformanceRankingEntry> monthlySalesRanking;
    private Integer totalAssignedCustomers;
    private Integer totalActiveCustomers;
    private Integer totalInactiveCustomers;
    private BigDecimal totalCollectionsPending;
    private BigDecimal totalMonthlySales;
    private BigDecimal totalIncentivePayout;
}