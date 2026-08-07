package com.business.erp.performance.engine;

import com.business.erp.performance.entity.IncentiveSlab;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Builder
public class PerformanceCalculationContext {
    private final Long employeeId;
    private final BigDecimal monthlyTarget;
    private final BigDecimal actualSales;
    private final List<IncentiveSlab> slabs;
}