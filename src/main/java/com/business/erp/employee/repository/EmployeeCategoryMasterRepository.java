package com.business.erp.employee.repository;

import com.business.erp.employee.entity.EmployeeCategoryMaster;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EmployeeCategoryMasterRepository extends JpaRepository<EmployeeCategoryMaster, Long> {
    Optional<EmployeeCategoryMaster> findByCode(String code);
    boolean existsByCode(String code);
    List<EmployeeCategoryMaster> findByActiveTrue();
}