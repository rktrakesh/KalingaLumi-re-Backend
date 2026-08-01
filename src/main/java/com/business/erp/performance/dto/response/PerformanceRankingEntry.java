package com.business.erp.performance.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class PerformanceRankingEntry {
    private Long employeeId;
    private String employeeName;
    private BigDecimal monthlyTarget;
    private BigDecimal monthlySales;
    private BigDecimal achievementPct;
    private BigDecimal incentiveEarned;
    private String recommendationLabel;
}