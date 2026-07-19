package com.business.erp.sales.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class SaleReturnRequest {
    @NotNull
    private Long materialId;
    @NotNull
    private BigDecimal quantityReturned;
    @NotNull
    private BigDecimal returnAmount;
    @NotNull
    private LocalDate returnDate;
    private String remarks;
}
