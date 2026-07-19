package com.business.erp.sales.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
public class SaleResponse {
    private Long id;
    private String invoiceReference;
    private Long customerId;
    private String customerName;
    private LocalDate invoiceDate;
    private LocalDate dueDate;
    private BigDecimal totalAmount;
    private BigDecimal paidAmount;
    private BigDecimal outstandingAmount;
    private String paymentStatus;
    private String status;
    private String remarks;
    private List<SaleItemResponse> items;

    @Data
    @Builder
    public static class SaleItemResponse {
        private Long id;
        private Long materialId;
        private String materialName;
        private BigDecimal quantityKg;
        private BigDecimal unitRate;
        private BigDecimal totalAmount;
    }
}
