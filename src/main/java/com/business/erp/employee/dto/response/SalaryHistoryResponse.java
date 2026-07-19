package com.business.erp.employee.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
public class SalaryHistoryResponse {
    private Long id;
    private BigDecimal salary;
    private LocalDate effectiveFrom;
    private String remarks;
    private String createdBy;
    private LocalDateTime createdDate;
}
