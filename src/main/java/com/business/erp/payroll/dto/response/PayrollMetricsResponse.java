package com.business.erp.payroll.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class PayrollMetricsResponse {
    private int employeesProcessed;
    private int employeesSkipped;
    private long executionDurationMillis;

    private BigDecimal totalPayroll;
    private BigDecimal totalOt;
    private BigDecimal totalHolidayOt;
    private BigDecimal totalWeeklyOffPay;
    private BigDecimal totalLeaveEncashment;
    private BigDecimal totalLossOfPay;

    private BigDecimal averageSalary;
    private BigDecimal averageOt;
    private BigDecimal averageLeaveDays;
}