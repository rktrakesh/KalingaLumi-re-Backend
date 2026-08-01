package com.business.erp.performance.service.impl;

import com.business.erp.employee.entity.Employee;
import com.business.erp.employee.enums.EmployeeCategory;
import com.business.erp.employee.service.EmployeeService;
import com.business.erp.performance.dto.response.EmployeePerformanceDashboardResponse;
import com.business.erp.performance.dto.response.ManagementPerformanceDashboardResponse;
import com.business.erp.performance.dto.response.PerformanceRankingEntry;
import com.business.erp.performance.service.PerformanceDashboardService;
import com.business.erp.performance.service.PerformanceLiveMetrics;
import com.business.erp.performance.service.PerformanceSnapshotService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PerformanceDashboardServiceImpl implements PerformanceDashboardService {

    private final PerformanceSnapshotService snapshotService;
    private final EmployeeService employeeService;
    private final Logger log = LoggerFactory.getLogger(PerformanceDashboardServiceImpl.class);

    @Override
    public EmployeePerformanceDashboardResponse getEmployeeDashboard(Long employeeId, int year, int month) {
        PerformanceLiveMetrics m = snapshotService.computeLiveMetrics(employeeId, year, month);
        int inactive = Math.max(0, m.assignedCustomerCount() - m.activeCustomerCount());

        return EmployeePerformanceDashboardResponse.builder()
                .employeeId(m.employee().getId()).employeeName(m.employee().getName())
                .periodYear(year).periodMonth(month)
                .monthlyTarget(m.policy().getMonthlyTarget())
                .monthlySales(m.actualSales())
                .achievementPct(m.result().getAchievementPct())
                .incentiveEarned(m.result().getIncentiveAmount())
                .assignedCustomers(m.assignedCustomerCount())
                .activeCustomers(m.activeCustomerCount())
                .inactiveCustomers(inactive)
                .ordersThisMonth(m.ordersCount())
                .averageOrderValue(m.averageOrderValue())
                .collectionPending(m.collectionPending())
                .newCustomers(m.newCustomersCount())
                .repeatCustomers(m.repeatCustomersCount())
                .largestOrder(m.largestOrder())
                .recommendationLabel(m.recommendation().label())
                .recommendationSuggestion(m.recommendation().suggestion())
                .build();
    }

    @Override
    public ManagementPerformanceDashboardResponse getManagementDashboard(int year, int month) {
        List<Employee> salesEmployees = employeeService.getActiveEmployees().stream()
                .filter(e -> e.getEmployeeCategory() == EmployeeCategory.SALES)
                .toList();

        List<PerformanceLiveMetrics> allMetrics = new ArrayList<>(salesEmployees.size());
        for (Employee emp : salesEmployees) {
            try {
                allMetrics.add(snapshotService.computeLiveMetrics(emp.getId(), year, month));
            } catch (Exception ex) {
                // No sales policy configured yet, etc. — excluded from the dashboard, not a failure.
                log.warn("PerformanceDashboardServiceImpl:getManagementDashboard :: SKIPPED empId={} reason={}",
                        emp.getId(), ex.getMessage());
            }
        }

        List<PerformanceRankingEntry> byAchievementDesc = allMetrics.stream()
                .sorted(Comparator.comparing((PerformanceLiveMetrics m) -> m.result().getAchievementPct()).reversed())
                .map(this::toRankingEntry).toList();
        List<PerformanceRankingEntry> byAchievementAsc = allMetrics.stream()
                .sorted(Comparator.comparing(m -> m.result().getAchievementPct()))
                .map(this::toRankingEntry).toList();
        List<PerformanceRankingEntry> bySalesDesc = allMetrics.stream()
                .sorted(Comparator.comparing((PerformanceLiveMetrics m) -> m.actualSales()).reversed())
                .map(this::toRankingEntry).toList();

        int totalAssigned = allMetrics.stream().mapToInt(PerformanceLiveMetrics::assignedCustomerCount).sum();
        int totalActive = allMetrics.stream().mapToInt(PerformanceLiveMetrics::activeCustomerCount).sum();
        BigDecimal totalCollectionsPending = allMetrics.stream().map(PerformanceLiveMetrics::collectionPending)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalMonthlySales = allMetrics.stream().map(PerformanceLiveMetrics::actualSales)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalIncentivePayout = allMetrics.stream().map(m -> m.result().getIncentiveAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return ManagementPerformanceDashboardResponse.builder()
                .periodYear(year).periodMonth(month)
                .topPerformers(byAchievementDesc.stream().limit(5).toList())
                .lowestPerformers(byAchievementAsc.stream().limit(5).toList())
                .targetAchievementRanking(byAchievementDesc)
                .monthlySalesRanking(bySalesDesc)
                .totalAssignedCustomers(totalAssigned)
                .totalActiveCustomers(totalActive)
                .totalInactiveCustomers(Math.max(0, totalAssigned - totalActive))
                .totalCollectionsPending(totalCollectionsPending)
                .totalMonthlySales(totalMonthlySales)
                .totalIncentivePayout(totalIncentivePayout)
                .build();
    }

    private PerformanceRankingEntry toRankingEntry(PerformanceLiveMetrics m) {
        return PerformanceRankingEntry.builder()
                .employeeId(m.employee().getId()).employeeName(m.employee().getName())
                .monthlyTarget(m.policy().getMonthlyTarget())
                .monthlySales(m.actualSales())
                .achievementPct(m.result().getAchievementPct())
                .incentiveEarned(m.result().getIncentiveAmount())
                .recommendationLabel(m.recommendation().label())
                .build();
    }
}