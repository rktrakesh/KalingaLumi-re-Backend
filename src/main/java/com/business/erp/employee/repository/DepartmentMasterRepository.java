package com.business.erp.employee.repository;

import com.business.erp.employee.entity.DepartmentMaster;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DepartmentMasterRepository extends JpaRepository<DepartmentMaster, Long> {
    Optional<DepartmentMaster> findByCode(String code);
    boolean existsByCode(String code);
    List<DepartmentMaster> findByActiveTrue();
}