package com.business.erp.purchase.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class CreatePurchaseRequest {
    @NotNull
    private Long supplierId;
    @NotNull
    private LocalDate purchaseDate;
    @NotEmpty
    private List<PurchaseItemRequest> items;
    private String remarks;

    @Data
    public static class PurchaseItemRequest {
        @NotNull
        private Long materialId;
        @NotNull
        private java.math.BigDecimal quantity;
        @NotNull
        private java.math.BigDecimal unitRate;
    }
}
