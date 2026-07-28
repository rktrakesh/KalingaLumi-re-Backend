package com.business.erp.payroll.engine;

import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;

@Value
@Builder
public class PayrollMetrics {
    int employeesProcessed;
    int employeesSkipped;
    long executionDurationMillis;

    BigDecimal totalPayroll;
    BigDecimal totalOt;
    BigDecimal totalHolidayOt;
    BigDecimal totalWeeklyOffPay;
    BigDecimal totalLeaveEncashment;
    BigDecimal totalLossOfPay;

    BigDecimal averageSalary;
    BigDecimal averageOt;
    BigDecimal averageLeaveDays;
}