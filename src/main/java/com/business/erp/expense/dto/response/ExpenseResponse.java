package com.business.erp.expense.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
public class ExpenseResponse {
    private Long id;
    private String expenseReference;
    private LocalDate expenseDate;
    private BigDecimal amount;
    private String category;
    private String remarks;
    private String status;
    private String approvedBy;
    private LocalDateTime approvedDate;
    private String createdBy;
    private LocalDateTime createdDate;
}
