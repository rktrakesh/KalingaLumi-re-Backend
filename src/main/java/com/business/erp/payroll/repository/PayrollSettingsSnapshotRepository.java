package com.business.erp.payroll.repository;

import com.business.erp.payroll.entity.PayrollSettingsSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PayrollSettingsSnapshotRepository extends JpaRepository<PayrollSettingsSnapshot, Long> {
    Optional<PayrollSettingsSnapshot> findByPayrollRunId(Long payrollRunId);
}