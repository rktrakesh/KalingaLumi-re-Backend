package com.business.erp.payroll.repository;

import com.business.erp.payroll.entity.PayrollRun;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PayrollRunRepository extends JpaRepository<PayrollRun, Long> {
    Optional<PayrollRun> findByYearAndMonth(int year, int month);

    boolean existsByYearAndMonth(int year, int month);

    List<PayrollRun> findAllByOrderByYearDescMonthDesc();
}
