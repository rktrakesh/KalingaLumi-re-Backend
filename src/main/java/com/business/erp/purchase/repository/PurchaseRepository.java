package com.business.erp.purchase.repository;

import com.business.erp.purchase.entity.Purchase;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;

public interface PurchaseRepository extends JpaRepository<Purchase, Long> {
    @Query("SELECT p FROM Purchase p WHERE " +
            "(:supplierId IS NULL OR p.supplier.id = :supplierId) AND " +
            "(:status IS NULL OR p.paymentStatus = :status) AND " +
            "(:from IS NULL OR p.purchaseDate >= :from) AND " +
            "(:to IS NULL OR p.purchaseDate <= :to) " +
            "ORDER BY p.purchaseDate DESC")
    Page<Purchase> search(@Param("supplierId") Long supplierId,
                          @Param("status") Purchase.PaymentStatus status,
                          @Param("from") LocalDate from,
                          @Param("to") LocalDate to,
                          Pageable pageable);
}
