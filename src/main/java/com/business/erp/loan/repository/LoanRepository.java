package com.business.erp.loan.repository;

import com.business.erp.loan.entity.EmployeeLoan;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface LoanRepository extends JpaRepository<EmployeeLoan, Long> {
    Optional<EmployeeLoan> findByEmployeeIdAndStatus(Long empId, EmployeeLoan.LoanStatus status);

    boolean existsByEmployeeIdAndStatus(Long empId, EmployeeLoan.LoanStatus status);

    List<EmployeeLoan> findByStatus(EmployeeLoan.LoanStatus status);

    @Query("SELECT l FROM EmployeeLoan l WHERE " +
            "(:empId IS NULL OR l.employee.id = :empId) AND " +
            "(:status IS NULL OR l.status = :status) ORDER BY l.createdDate DESC")
    Page<EmployeeLoan> search(@Param("empId") Long empId,
                              @Param("status") EmployeeLoan.LoanStatus status,
                              Pageable pageable);
}
