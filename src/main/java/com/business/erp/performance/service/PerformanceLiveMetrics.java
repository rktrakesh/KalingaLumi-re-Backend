package com.business.erp.performance.service;

import com.business.erp.employee.entity.Employee;
import com.business.erp.performance.engine.PerformanceCalculationResult;
import com.business.erp.performance.entity.EmployeeSalesPolicy;
import com.business.erp.performance.recommendation.PerformanceRecommendation;

import java.math.BigDecimal;

public record PerformanceLiveMetrics(
        Employee employee,
        EmployeeSalesPolicy policy,
        PerformanceCalculationResult result,
        PerformanceRecommendation recommendation,
        BigDecimal actualSales,
        int assignedCustomerCount,
        int activeCustomerCount,
        int ordersCount,
        BigDecimal totalOrderValue,
        BigDecimal averageOrderValue,
        int newCustomersCount,
        int repeatCustomersCount,
        BigDecimal collectionPending,
        BigDecimal largestOrder
) {
}