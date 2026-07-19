package com.business.erp.production.repository;

import com.business.erp.production.entity.ProductionBatch;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;

public interface ProductionBatchRepository extends JpaRepository<ProductionBatch, Long> {
    @Query("SELECT b FROM ProductionBatch b WHERE " +
            "(:status IS NULL OR b.status = :status) AND " +
            "(:from IS NULL OR b.batchDate >= :from) AND " +
            "(:to IS NULL OR b.batchDate <= :to) ORDER BY b.batchDate DESC")
    Page<ProductionBatch> search(@Param("status") ProductionBatch.BatchStatus status,
                                 @Param("from") LocalDate from,
                                 @Param("to") LocalDate to,
                                 Pageable pageable);
}
