package com.business.erp.performance.recommendation;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class RecommendationEngine {

    private static final BigDecimal OUTSTANDING_THRESHOLD = BigDecimal.valueOf(200);
    private static final BigDecimal EXCELLENT_THRESHOLD = BigDecimal.valueOf(100);
    private static final BigDecimal CLOSE_THRESHOLD = BigDecimal.valueOf(90);

    public PerformanceRecommendation recommend(BigDecimal achievementPct) {
        if (achievementPct.compareTo(OUTSTANDING_THRESHOLD) >= 0) {
            return new PerformanceRecommendation("OUTSTANDING_PERFORMER", "Outstanding Performer", "Employee of the Month");
        }
        if (achievementPct.compareTo(EXCELLENT_THRESHOLD) >= 0) {
            return new PerformanceRecommendation("EXCELLENT_PERFORMANCE", "Excellent Performance", null);
        }
        if (achievementPct.compareTo(CLOSE_THRESHOLD) >= 0) {
            return new PerformanceRecommendation("VERY_CLOSE_TO_TARGET", "Very Close to Target", "Encourage Employee");
        }
        return new PerformanceRecommendation("NEEDS_IMPROVEMENT", "Needs Improvement", "Schedule Performance Review");
    }
}