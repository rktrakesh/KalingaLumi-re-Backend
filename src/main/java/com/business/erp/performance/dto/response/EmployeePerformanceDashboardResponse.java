package com.business.erp.performance.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class EmployeePerformanceDashboardResponse {
    private Long employeeId;
    private String employeeName;
    private Integer periodYear;
    private Integer periodMonth;
    private BigDecimal monthlyTarget;
    private BigDecimal monthlySales;
    private BigDecimal achievementPct;
    private BigDecimal incentiveEarned;
    private Integer assignedCustomers;
    private Integer activeCustomers;
    private Integer inactiveCustomers;
    private Integer ordersThisMonth;
    private BigDecimal averageOrderValue;
    private BigDecimal collectionPending;
    private Integer newCustomers;
    private Integer repeatCustomers;
    private BigDecimal largestOrder;
    private String recommendationLabel;
    private String recommendationSuggestion;
}