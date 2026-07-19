package com.business.erp.sales.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
public class CreateSaleRequest {
    @NotNull
    private Long customerId;
    @NotNull
    private LocalDate invoiceDate;
    @NotEmpty
    private List<SaleItemRequest> items;
    private String remarks;

    @Data
    public static class SaleItemRequest {
        @NotNull
        private Long materialId;
        @NotNull
        private BigDecimal quantityKg;
        @NotNull
        private BigDecimal unitRate;
    }
}
