package com.business.erp.purchase.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
public class PurchaseResponse {
    private Long id;
    private String purchaseReference;
    private Long supplierId;
    private String supplierName;
    private LocalDate purchaseDate;
    private BigDecimal totalAmount;
    private BigDecimal paidAmount;
    private BigDecimal outstandingAmount;
    private String paymentStatus;
    private String status;
    private String remarks;
    private List<PurchaseItemResponse> items;

    @Data
    @Builder
    public static class PurchaseItemResponse {
        private Long id;
        private Long materialId;
        private String materialName;
        private BigDecimal quantity;
        private BigDecimal unitRate;
        private BigDecimal totalAmount;
    }
}
