package com.business.erp.payroll.repository;

import com.business.erp.payroll.entity.PayrollCalculationLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PayrollCalculationLogRepository extends JpaRepository<PayrollCalculationLog, Long> {

    List<PayrollCalculationLog> findByPayrollRunId(Long payrollRunId);

    Optional<PayrollCalculationLog> findByPayrollRunIdAndEmployeeId(Long payrollRunId, Long employeeId);

    List<PayrollCalculationLog> findByEmployeeIdOrderByCalculatedDateDesc(Long employeeId);
}