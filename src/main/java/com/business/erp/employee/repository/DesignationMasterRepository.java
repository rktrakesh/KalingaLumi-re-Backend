package com.business.erp.employee.repository;

import com.business.erp.employee.entity.DesignationMaster;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DesignationMasterRepository extends JpaRepository<DesignationMaster, Long> {
    Optional<DesignationMaster> findByCode(String code);
    boolean existsByCode(String code);
    List<DesignationMaster> findByActiveTrue();
    List<DesignationMaster> findByCategoryIdAndActiveTrue(Long categoryId);
}