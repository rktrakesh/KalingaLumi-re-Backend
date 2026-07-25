package com.business.erp.payroll.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class PayrollDashboardResponse {
    private Long payrollRunId;
    private int year;
    private int month;
    private String status;

    private int totalEmployees;
    private BigDecimal totalBasicSalary;
    private BigDecimal totalOvertime;
    private BigDecimal totalWeeklyOffAmount;
    private BigDecimal totalHolidayPay;
    private BigDecimal totalLeaveEncashment;
    private BigDecimal totalLossOfPay;
    private BigDecimal grandTotal;

    private long paidCount;
    private long pendingCount;
}