package com.business.erp.performance.repository;

import com.business.erp.performance.entity.PerformanceSnapshot;
import com.business.erp.performance.enums.PerformanceSnapshotStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PerformanceSnapshotRepository extends JpaRepository<PerformanceSnapshot, Long> {

    Optional<PerformanceSnapshot> findByEmployeeIdAndPeriodYearAndPeriodMonth(Long employeeId, Integer periodYear, Integer periodMonth);

    Optional<PerformanceSnapshot> findByEmployeeIdAndPeriodYearAndPeriodMonthAndStatus(
            Long employeeId, Integer periodYear, Integer periodMonth, PerformanceSnapshotStatus status);

    List<PerformanceSnapshot> findByPeriodYearAndPeriodMonth(Integer periodYear, Integer periodMonth);

    List<PerformanceSnapshot> findByEmployeeIdOrderByPeriodYearDescPeriodMonthDesc(Long employeeId);
}