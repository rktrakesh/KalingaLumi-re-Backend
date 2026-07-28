package com.business.erp.payroll.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class PayrollCalculationLogResponse {
    private Long id;
    private Long payrollRunId;
    private Long employeeId;
    private String employeeCode;
    private String employeeName;
    private Integer calculationVersion;

    private BigDecimal monthlySalary;
    private BigDecimal hourlyRate;

    private Integer presentDays;
    private Integer weeklyOffDays;
    private Integer weeklyOffWorkedDays;
    private Integer holidayDays;
    private Integer holidayWorkedDays;
    private Integer paidLeaveDays;
    private Integer automaticPaidLeaveDays;
    private Integer absentDays;

    private Integer approvedOtMinutes;
    private Integer holidayOtMinutes;
    private Integer weeklyOffOtMinutes;

    private BigDecimal basicSalaryAmount;
    private BigDecimal overtimeAmount;
    private BigDecimal weeklyOffPayAmount;
    private BigDecimal holidayOtAmount;
    private BigDecimal leaveEncashmentDays;
    private BigDecimal leaveEncashmentAmount;
    private BigDecimal lossOfPayAmount;

    private BigDecimal grossSalary;
    private BigDecimal finalNetSalary;

    private String calculatedBy;
    private LocalDateTime calculatedDate;
    private java.util.List<String> breakdown;
}