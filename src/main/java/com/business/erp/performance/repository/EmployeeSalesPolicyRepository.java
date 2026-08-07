package com.business.erp.performance.repository;

import com.business.erp.performance.entity.EmployeeSalesPolicy;
import com.business.erp.performance.enums.SalesPolicyStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface EmployeeSalesPolicyRepository extends JpaRepository<EmployeeSalesPolicy, Long> {

    @EntityGraph(attributePaths = {"employee", "incentiveSlabs"})
    Optional<EmployeeSalesPolicy> findByEmployeeIdAndStatus(Long employeeId, SalesPolicyStatus status);

    /** The policy version that was in effect on a given date — used by snapshot generation so a
     *  later target change never changes what a past payroll period was calculated against. */
    @Query("SELECT p FROM EmployeeSalesPolicy p WHERE p.employee.id = :employeeId " +
            "AND p.effectiveFrom <= :asOf AND (p.effectiveTo IS NULL OR p.effectiveTo >= :asOf)")
    Optional<EmployeeSalesPolicy> findPolicyAsOf(@Param("employeeId") Long employeeId, @Param("asOf") LocalDate asOf);

    @EntityGraph(attributePaths = {"employee", "incentiveSlabs"})
    List<EmployeeSalesPolicy> findByEmployeeIdOrderByVersionDesc(Long employeeId);
}
