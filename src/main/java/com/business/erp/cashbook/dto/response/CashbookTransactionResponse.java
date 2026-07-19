package com.business.erp.cashbook.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
public class CashbookTransactionResponse {
    private Long id;
    private Long accountId;
    private String accountName;
    private LocalDate transactionDate;
    private String transactionType;
    private String flowType;
    private BigDecimal amount;
    private BigDecimal balanceAfter;
    private String referenceType;
    private Long referenceId;
    private String description;
    private String createdBy;
    private LocalDateTime createdDate;
}
