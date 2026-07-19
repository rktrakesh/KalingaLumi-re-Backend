package com.business.erp.inventory.repository;

import com.business.erp.inventory.entity.InventoryLedger;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.Optional;

public interface InventoryLedgerRepository extends JpaRepository<InventoryLedger, Long> {

    @Query("SELECT il FROM InventoryLedger il WHERE il.material.id = :id ORDER BY il.id DESC LIMIT 1")
    Optional<InventoryLedger> findLatestByMaterial(@Param("id") Long materialId);

    Page<InventoryLedger> findByMaterialIdOrderByTransactionDateDescIdDesc(Long materialId, Pageable pageable);

    @Query("SELECT COALESCE(SUM(il.quantity), 0) FROM InventoryLedger il WHERE il.material.id = :id")
    BigDecimal sumQuantityByMaterial(@Param("id") Long materialId);
}
