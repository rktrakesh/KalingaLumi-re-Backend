package com.business.erp.performance.recommendation;

/**
 * @param code       machine-readable code, stable for UI/report filtering
 * @param label      short display label
 * @param suggestion optional suggested management action; null when there's none
 */
public record PerformanceRecommendation(String code, String label, String suggestion) {
}