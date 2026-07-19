package com.business.erp.sales.repository;

import com.business.erp.sales.entity.SalesInvoice;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;

public interface SalesInvoiceRepository extends JpaRepository<SalesInvoice, Long> {
    @Query("SELECT s FROM SalesInvoice s WHERE " +
            "(:customerId IS NULL OR s.customer.id = :customerId) AND " +
            "(:status IS NULL OR s.paymentStatus = :status) AND " +
            "(:from IS NULL OR s.invoiceDate >= :from) AND " +
            "(:to IS NULL OR s.invoiceDate <= :to) ORDER BY s.invoiceDate DESC")
    Page<SalesInvoice> search(@Param("customerId") Long customerId,
                              @Param("status") SalesInvoice.PaymentStatus status,
                              @Param("from") LocalDate from,
                              @Param("to") LocalDate to,
                              Pageable pageable);

    @Query("SELECT COALESCE(SUM(s.totalAmount), 0) FROM SalesInvoice s WHERE s.status = 'ACTIVE' " +
            "AND s.invoiceDate BETWEEN :from AND :to")
    BigDecimal sumRevenueByPeriod(@Param("from") LocalDate from, @Param("to") LocalDate to);
}
