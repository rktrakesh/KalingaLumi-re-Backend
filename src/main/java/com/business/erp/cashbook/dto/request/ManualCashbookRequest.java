package com.business.erp.cashbook.dto.request;

import com.business.erp.cashbook.entity.CashbookTransaction;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class ManualCashbookRequest {
    @NotNull
    private Long accountId;
    @NotNull
    private LocalDate transactionDate;
    @NotNull
    private CashbookTransaction.TransactionType transactionType;
    @NotNull
    private CashbookTransaction.FlowType flowType;
    @NotNull
    @DecimalMin("0.01")
    private BigDecimal amount;
    private String description;
}
