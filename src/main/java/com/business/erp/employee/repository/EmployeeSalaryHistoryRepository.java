package com.business.erp.employee.repository;

import com.business.erp.employee.entity.EmployeeSalaryHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface EmployeeSalaryHistoryRepository extends JpaRepository<EmployeeSalaryHistory, Long> {
    List<EmployeeSalaryHistory> findByEmployeeIdOrderByEffectiveFromDesc(Long employeeId);

    @Query("SELECT h FROM EmployeeSalaryHistory h WHERE h.employee.id = :empId " +
            "AND h.effectiveFrom <= :asOf ORDER BY h.effectiveFrom DESC")
    List<EmployeeSalaryHistory> findAsOf(@Param("empId") Long empId, @Param("asOf") LocalDate asOf);

    default Optional<EmployeeSalaryHistory> findSalaryAsOf(Long empId, LocalDate asOf) {
        List<EmployeeSalaryHistory> r = findAsOf(empId, asOf);
        return r.isEmpty() ? Optional.empty() : Optional.of(r.get(0));
    }
}
