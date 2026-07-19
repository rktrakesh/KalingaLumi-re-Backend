package com.business.erp.supplier.repository;

import com.business.erp.supplier.entity.Supplier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SupplierRepository extends JpaRepository<Supplier, Long> {
    @Query("SELECT s FROM Supplier s WHERE (:status IS NULL OR s.status = :status) AND " +
            "(:search IS NULL OR LOWER(s.name) LIKE LOWER(CONCAT('%',:search,'%')))")
    Page<Supplier> search(@Param("status") Supplier.SupplierStatus status,
                          @Param("search") String search, Pageable pageable);
}
