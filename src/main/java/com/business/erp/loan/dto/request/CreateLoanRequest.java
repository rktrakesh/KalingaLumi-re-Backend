package com.business.erp.loan.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class CreateLoanRequest {
    @NotNull
    private Long employeeId;
    @NotNull
    @DecimalMin("1.00")
    private BigDecimal principalAmount;
    @NotNull
    @DecimalMin("0.0")
    private BigDecimal interestRate;
    @NotNull
    @DecimalMin("1.00")
    private BigDecimal monthlyPrincipalPayment;
    private String remarks;
}
