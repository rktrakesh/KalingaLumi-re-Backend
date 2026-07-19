package com.business.erp.supplier.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
public class SupplierLedgerResponse {
    private Long id;
    private LocalDate transactionDate;
    private String transactionType;
    private BigDecimal amount;
    private BigDecimal balanceAfter;
    private String remarks;
    private String createdBy;
}
