package com.business.erp.performance.engine;

import com.business.erp.common.exception.BusinessException;
import com.business.erp.performance.entity.IncentiveSlab;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class PerformanceCalculationEngine {

    private final IncentiveSlabResolver slabResolver;

    public PerformanceCalculationResult calculate(PerformanceCalculationContext ctx) {
        BigDecimal target = ctx.getMonthlyTarget();
        BigDecimal sales = ctx.getActualSales() != null ? ctx.getActualSales() : BigDecimal.ZERO;
        List<String> breakdown = new ArrayList<>();

        if (target == null || target.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("INVALID_TARGET: monthly target must be greater than zero for employeeId=" + ctx.getEmployeeId());
        }

        BigDecimal achievementPct = sales.divide(target, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100)).setScale(2, RoundingMode.HALF_UP);
        breakdown.add("Achievement = (Sales " + sales + " / Target " + target + ") x 100 = " + achievementPct + "%");

        BigDecimal extraSales = sales.subtract(target).max(BigDecimal.ZERO);
        Optional<IncentiveSlab> slabOpt = slabResolver.resolve(ctx.getSlabs(), achievementPct);

        if (slabOpt.isEmpty()) {
            breakdown.add("No configured incentive slab matched " + achievementPct + "% achievement — incentive = 0");
            return PerformanceCalculationResult.builder()
                    .achievementPct(achievementPct)
                    .extraSales(BigDecimal.ZERO)
                    .incentivePctApplied(BigDecimal.ZERO)
                    .incentiveAmount(BigDecimal.ZERO)
                    .breakdown(breakdown)
                    .build();
        }

        IncentiveSlab slab = slabOpt.get();
        BigDecimal incentiveAmount = extraSales.multiply(slab.getIncentivePct())
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

        breakdown.add("Extra Sales = Sales " + sales + " - Target " + target + " = " + extraSales);
        breakdown.add("Applied Slab " + slab.getMinAchievementPct() + "-" +
                (slab.getMaxAchievementPct() == null ? "above" : slab.getMaxAchievementPct()) +
                "% @ " + slab.getIncentivePct() + "%");
        breakdown.add("Incentive = Extra Sales " + extraSales + " x " + slab.getIncentivePct() + "% = " + incentiveAmount);

        return PerformanceCalculationResult.builder()
                .achievementPct(achievementPct)
                .extraSales(extraSales)
                .appliedSlabId(slab.getId())
                .incentivePctApplied(slab.getIncentivePct())
                .incentiveAmount(incentiveAmount)
                .breakdown(breakdown)
                .build();
    }
}