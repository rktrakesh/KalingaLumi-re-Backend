package com.business.erp.performance.engine;

import com.business.erp.performance.entity.IncentiveSlab;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Component
public class IncentiveSlabResolver {

    /** Highest-ordered slab whose [min, max] band contains {@code achievementPct}.
     *  Empty if achievement falls below every configured slab (i.e. no incentive). */
    public Optional<IncentiveSlab> resolve(List<IncentiveSlab> slabs, BigDecimal achievementPct) {
        return slabs.stream()
                .filter(s -> achievementPct.compareTo(s.getMinAchievementPct()) >= 0)
                .filter(s -> s.getMaxAchievementPct() == null || achievementPct.compareTo(s.getMaxAchievementPct()) <= 0)
                .max(Comparator.comparing(IncentiveSlab::getSlabOrder));
    }
}