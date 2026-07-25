package com.business.erp.payroll.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
public class PayrollDetailResponse {
    private Long id;
    private Long employeeId;
    private String employeeCode;
    private String employeeName;
    private Integer calculationVersion;
    private BigDecimal baseSalary;
    private Integer standardWorkDays;
    private Integer standardWorkHours;
    private BigDecimal hourlyRate;

    private Integer presentDays;
    private Integer weeklyOffDays;
    private Integer weeklyOffWorkedDays;
    private Integer holidayDays;
    private Integer holidayWorkedDays;
    private Integer paidLeaveDays;
    private Integer automaticPaidLeaveDays;
    private Integer absentDays;

    private Integer workedMinutes;
    private Integer overtimeMinutes;
    private BigDecimal overtimeMultiplier;
    private Integer holidayOtMinutes;
    private Integer weeklyOffOtMinutes;
    private BigDecimal weeklyOffMultiplier;
    private BigDecimal holidayOtMultiplier;

    private BigDecimal weeklyOffPay;
    private BigDecimal holidayOtPay;
    private BigDecimal overtimePay;
    private BigDecimal leaveEncashmentDays;
    private BigDecimal leaveEncashmentAmount;
    private BigDecimal lossOfPayAmount;

    private BigDecimal grossSalary;
    private BigDecimal loanInterestDeduction;
    private BigDecimal loanPrincipalDeduction;
    private BigDecimal totalDeductions;
    private BigDecimal netSalary;
    private Boolean salaryCapped;
    private String paymentStatus;
    private LocalDate paidDate;
    private String paymentMode;
    private String paidBy;
}