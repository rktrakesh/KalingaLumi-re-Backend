package com.business.erp.employee.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class UpdateSalaryRequest {
    @NotNull
    @DecimalMin("0.01")
    private BigDecimal newSalary;
    @NotNull
    private LocalDate effectiveFrom;
    private String remarks;
}
