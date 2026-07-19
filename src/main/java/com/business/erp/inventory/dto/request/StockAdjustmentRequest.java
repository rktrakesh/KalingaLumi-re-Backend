package com.business.erp.inventory.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class StockAdjustmentRequest {
    @NotNull
    private Long materialId;
    @NotNull
    private BigDecimal quantity;   // positive = add, negative = remove
    @NotNull
    private LocalDate adjustmentDate;
    @NotNull
    private String remarks;
    private BigDecimal unitCost;
}
