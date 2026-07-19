package com.business.erp.payroll.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class PayrollDetailResponse {
    private Long id;
    private Long employeeId;
    private String employeeCode;
    private String employeeName;
    private BigDecimal baseSalary;
    private Integer standardWorkDays;
    private Integer standardWorkHours;
    private BigDecimal hourlyRate;
    private Integer workedMinutes;
    private Integer paidLeaveDays;
    private Integer overtimeMinutes;
    private BigDecimal overtimeMultiplier;
    private BigDecimal grossSalary;
    private BigDecimal loanInterestDeduction;
    private BigDecimal loanPrincipalDeduction;
    private BigDecimal totalDeductions;
    private BigDecimal netSalary;
    private Boolean salaryCapped;
    private String paymentStatus;
}
