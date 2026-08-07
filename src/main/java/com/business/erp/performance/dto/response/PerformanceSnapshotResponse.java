package com.business.erp.performance.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class PerformanceSnapshotResponse {
    private Long id;
    private Long employeeId;
    private String employeeName;
    private Long payrollRunId;
    private Integer periodYear;
    private Integer periodMonth;
    private Integer salesPolicyVersion;
    private BigDecimal monthlyTarget;
    private BigDecimal actualSales;
    private BigDecimal achievementPct;
    private BigDecimal incentivePctApplied;
    private BigDecimal incentiveAmount;
    private Integer assignedCustomerCount;
    private Integer activeCustomerCount;
    private Integer ordersCount;
    private BigDecimal totalOrderValue;
    private BigDecimal averageOrderValue;
    private Integer newCustomersCount;
    private Integer repeatCustomersCount;
    private String recommendationCode;
    private String recommendationLabel;
    private String recommendationSuggestion;
    private String status;
    private String engineVersion;
    private String generatedBy;
    private LocalDateTime generatedDate;
    private String approvedBy;
    private LocalDateTime approvedDate;
}