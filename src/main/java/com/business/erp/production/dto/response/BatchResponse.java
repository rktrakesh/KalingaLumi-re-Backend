package com.business.erp.production.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
public class BatchResponse {
    private Long id;
    private String batchNumber;
    private LocalDate batchDate;
    private String managerName;
    private String status;
    private String remarks;
    private List<InputResponse> inputs;
    private List<OutputResponse> outputs;

    @Data
    @Builder
    public static class InputResponse {
        private Long materialId;
        private String materialName;
        private BigDecimal quantityUsed;
    }

    @Data
    @Builder
    public static class OutputResponse {
        private Long materialId;
        private String materialName;
        private BigDecimal finishedQuantity;
        private BigDecimal wasteQuantity;
        private BigDecimal efficiencyPercent;
    }
}
