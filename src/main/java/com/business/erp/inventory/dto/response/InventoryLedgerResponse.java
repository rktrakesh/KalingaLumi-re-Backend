package com.business.erp.inventory.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
public class InventoryLedgerResponse {
    private Long id;
    private LocalDate transactionDate;
    private String transactionType;
    private BigDecimal quantity;
    private BigDecimal balanceAfter;
    private BigDecimal unitCost;
    private String referenceType;
    private Long referenceId;
    private String remarks;
    private String createdBy;
}
