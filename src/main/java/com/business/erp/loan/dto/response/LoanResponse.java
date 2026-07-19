package com.business.erp.loan.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
public class LoanResponse {
    private Long id;
    private String loanReference;
    private Long employeeId;
    private String employeeCode;
    private String employeeName;
    private BigDecimal principalAmount;
    private BigDecimal interestRate;
    private BigDecimal monthlyInterest;
    private BigDecimal monthlyPrincipalPayment;
    private BigDecimal currentBalance;
    private String status;
    private LocalDate disbursementDate;
    private String approvedBy;
    private LocalDateTime approvedDate;
    private String remarks;
    private LocalDateTime createdDate;
}
