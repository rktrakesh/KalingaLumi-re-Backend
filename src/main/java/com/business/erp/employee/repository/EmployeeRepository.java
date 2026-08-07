package com.business.erp.employee.repository;

import com.business.erp.employee.entity.Employee;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface EmployeeRepository extends JpaRepository<Employee, Long> {
    List<Employee> findByStatus(Employee.EmployeeStatus status);

    List<Employee> findByStatusIn(List<Employee.EmployeeStatus> statuses);

    Optional<Employee> findByEmail(String email);

    List<Employee> findAllByEmployeeCodeIgnoreCase(String employeeCode);

    @Query("SELECT e FROM Employee e WHERE (:status IS NULL OR e.status = :status) AND " +
            "(:search IS NULL OR LOWER(e.name) LIKE LOWER(CONCAT('%',:search,'%')) " +
            "OR LOWER(e.employeeCode) LIKE LOWER(CONCAT('%',:search,'%')))")
    Page<Employee> findWithFilters(@Param("status") Employee.EmployeeStatus status,
                                   @Param("search") String search, Pageable pageable);
}
