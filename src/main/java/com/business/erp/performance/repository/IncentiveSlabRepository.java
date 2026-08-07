package com.business.erp.performance.repository;

import com.business.erp.performance.entity.IncentiveSlab;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface IncentiveSlabRepository extends JpaRepository<IncentiveSlab, Long> {

    List<IncentiveSlab> findBySalesPolicyIdOrderBySlabOrderAsc(Long salesPolicyId);
}