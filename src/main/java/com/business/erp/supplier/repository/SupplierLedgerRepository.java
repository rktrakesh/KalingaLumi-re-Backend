package com.business.erp.supplier.repository;

import com.business.erp.supplier.entity.SupplierLedger;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SupplierLedgerRepository extends JpaRepository<SupplierLedger, Long> {
    List<SupplierLedger> findBySupplierIdOrderByTransactionDateAsc(Long supplierId);

    @Query("SELECT sl FROM SupplierLedger sl WHERE sl.supplier.id = :id ORDER BY sl.id DESC LIMIT 1")
    Optional<SupplierLedger> findLatest(@Param("id") Long supplierId);
}
