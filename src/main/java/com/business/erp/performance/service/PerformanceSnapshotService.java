package com.business.erp.performance.service;

import com.business.erp.performance.entity.PerformanceSnapshot;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;


public interface PerformanceSnapshotService {

    PerformanceSnapshot generate(Long employeeId, int year, int month, String generatedBy);

    PerformanceLiveMetrics computeLiveMetrics(Long employeeId, int year, int month);

    List<PerformanceSnapshot> generateForAllSalesEmployees(int year, int month, String generatedBy);

    PerformanceSnapshot approve(Long snapshotId, String approvedBy);

    PerformanceSnapshot linkToPayrollRun(Long snapshotId, Long payrollRunId);

    Optional<BigDecimal> getApprovedIncentiveAmount(Long employeeId, int year, int month);

    Optional<PerformanceSnapshot> findByEmployeeAndPeriod(Long employeeId, int year, int month);

    List<PerformanceSnapshot> findByPeriod(int year, int month);

    List<PerformanceSnapshot> findHistoryForEmployee(Long employeeId);
}