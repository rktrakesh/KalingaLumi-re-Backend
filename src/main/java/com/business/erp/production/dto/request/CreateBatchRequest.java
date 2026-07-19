package com.business.erp.production.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
public class CreateBatchRequest {
    @NotNull
    private LocalDate batchDate;
    @NotEmpty
    private List<InputItem> inputs;
    private String remarks;

    @Data
    public static class InputItem {
        @NotNull
        private Long materialId;
        @NotNull
        private BigDecimal quantityUsed;
    }
}
