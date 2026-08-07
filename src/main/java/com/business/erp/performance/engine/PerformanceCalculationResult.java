package com.business.erp.performance.engine;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Builder
public class PerformanceCalculationResult {
    private final BigDecimal achievementPct;
    private final BigDecimal extraSales;
    private final Long appliedSlabId;
    private final BigDecimal incentivePctApplied;
    private final BigDecimal incentiveAmount;
    private final List<String> breakdown;
}