package com.business.erp.performance.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
public class SalesPolicyResponse {
    private Long id;
    private Long employeeId;
    private String employeeName;
    private BigDecimal monthlyTarget;
    private LocalDate effectiveFrom;
    private LocalDate effectiveTo;
    private Integer version;
    private String status;
    private List<SlabResponse> slabs;

    @Data
    @Builder
    public static class SlabResponse {
        private Long id;
        private BigDecimal minAchievementPct;
        private BigDecimal maxAchievementPct;
        private BigDecimal incentivePct;
        private Integer slabOrder;
    }
}