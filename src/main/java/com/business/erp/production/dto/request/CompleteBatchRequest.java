package com.business.erp.production.dto.request;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class CompleteBatchRequest {
    @NotEmpty
    private List<OutputItem> outputs;
    private String remarks;

    @Data
    public static class OutputItem {
        private Long materialId;
        private BigDecimal finishedQuantity;
        private BigDecimal wasteQuantity;
    }
}
