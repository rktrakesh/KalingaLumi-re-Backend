package com.business.erp.payroll.repository;

import com.business.erp.payroll.entity.PayrollDetail;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PayrollDetailRepository extends JpaRepository<PayrollDetail, Long> {
    List<PayrollDetail> findByPayrollRunId(Long runId);

    Optional<PayrollDetail> findByPayrollRunIdAndEmployeeId(Long runId, Long empId);

    Optional<PayrollDetail> findByPayrollRun_YearAndPayrollRun_MonthAndEmployeeId(int year, int month, Long empId);

    void deleteByPayrollRunId(Long runId);
}
