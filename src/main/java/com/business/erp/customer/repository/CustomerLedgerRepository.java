package com.business.erp.customer.repository;

import com.business.erp.customer.entity.CustomerLedger;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface CustomerLedgerRepository extends JpaRepository<CustomerLedger, Long> {
    List<CustomerLedger> findByCustomerIdOrderByTransactionDateAsc(Long customerId);

    @Query("SELECT cl FROM CustomerLedger cl WHERE cl.customer.id = :id ORDER BY cl.id DESC LIMIT 1")
    Optional<CustomerLedger> findLatest(@Param("id") Long customerId);

    @Query("SELECT COALESCE(SUM(cl.balanceAfter), 0) FROM CustomerLedger cl " +
            "WHERE cl.customer.id = :id AND cl.id = (SELECT MAX(cl2.id) FROM CustomerLedger cl2 WHERE cl2.customer.id = :id)")
    Optional<BigDecimal> findCurrentBalance(@Param("id") Long customerId);
}
