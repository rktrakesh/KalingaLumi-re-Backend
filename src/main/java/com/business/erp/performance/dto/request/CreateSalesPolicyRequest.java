package com.business.erp.performance.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
public class CreateSalesPolicyRequest {
    @NotNull
    private Long employeeId;
    @NotNull
    @DecimalMin(value = "0.01", message = "Monthly target must be greater than zero")
    private BigDecimal monthlyTarget;
    @NotNull
    private LocalDate effectiveFrom;
    @NotEmpty(message = "At least one incentive slab is required")
    @Valid
    private List<SlabRequest> slabs;

    @Data
    public static class SlabRequest {
        @NotNull
        private BigDecimal minAchievementPct;
        /** Null = unbounded top slab (e.g. "Above 150%"). */
        private BigDecimal maxAchievementPct;
        @NotNull
        private BigDecimal incentivePct;
        @NotNull
        private Integer slabOrder;
    }
}