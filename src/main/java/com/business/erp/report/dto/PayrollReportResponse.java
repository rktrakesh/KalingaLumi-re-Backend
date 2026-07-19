package com.business.erp.report.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class PayrollReportResponse {
    private int year;
    private int month;
    private String runReference;
    private int totalEmployees;
    private BigDecimal totalGross;
    private BigDecimal totalNet;
    private BigDecimal totalDeductions;
    private List<PayrollDetailSummary> details;

    @Data
    @Builder
    public static class PayrollDetailSummary {
        private String employeeCode;
        private String employeeName;
        private BigDecimal baseSalary;
        private BigDecimal grossSalary;
        private BigDecimal totalDeductions;
        private BigDecimal netSalary;
        private boolean salaryCapped;
    }
}
