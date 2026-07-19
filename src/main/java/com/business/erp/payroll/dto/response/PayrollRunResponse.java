package com.business.erp.payroll.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
public class PayrollRunResponse {
    private Long id;
    private String runReference;
    private int year;
    private int month;
    private LocalDate periodStart;
    private LocalDate periodEnd;
    private String status;
    private Integer totalEmployees;
    private BigDecimal totalGross;
    private BigDecimal totalNet;
    private String generatedBy;
    private LocalDateTime generatedDate;
    private String remarks;
}
