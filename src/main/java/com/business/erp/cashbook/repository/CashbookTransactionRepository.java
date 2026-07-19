package com.business.erp.cashbook.repository;

import com.business.erp.cashbook.entity.CashbookTransaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

public interface CashbookTransactionRepository extends JpaRepository<CashbookTransaction, Long> {

    @Query("SELECT MAX(t.balanceAfter) FROM CashbookTransaction t WHERE t.account.id = :accountId " +
           "AND t.id = (SELECT MAX(t2.id) FROM CashbookTransaction t2 WHERE t2.account.id = :accountId)")
    Optional<BigDecimal> findCurrentBalance(@Param("accountId") Long accountId);

    @Query("SELECT t FROM CashbookTransaction t WHERE t.account.id = :accountId ORDER BY t.id DESC LIMIT 1")
    Optional<CashbookTransaction> findLatestByAccount(@Param("accountId") Long accountId);

    @Query("SELECT t FROM CashbookTransaction t WHERE " +
            "(:accountId IS NULL OR t.account.id = :accountId) AND " +
            "(:from IS NULL OR t.transactionDate >= :from) AND " +
            "(:to IS NULL OR t.transactionDate <= :to) AND " +
            "(:type IS NULL OR t.transactionType = :type) " +
            "ORDER BY t.transactionDate DESC, t.id DESC")
    Page<CashbookTransaction> search(@Param("accountId") Long accountId,
                                     @Param("from") LocalDate from,
                                     @Param("to") LocalDate to,
                                     @Param("type") CashbookTransaction.TransactionType type,
                                     Pageable pageable);

    @Query("SELECT COALESCE(SUM(CASE WHEN t.flowType = 'CREDIT' THEN t.amount ELSE 0 END), 0) " +
           "FROM CashbookTransaction t WHERE t.account.id = :accountId " +
           "AND t.transactionDate BETWEEN :from AND :to")
    BigDecimal sumCreditByAccountAndPeriod(@Param("accountId") Long accountId,
                                           @Param("from") LocalDate from,
                                           @Param("to") LocalDate to);

    @Query("SELECT COALESCE(SUM(CASE WHEN t.flowType = 'DEBIT' THEN t.amount ELSE 0 END), 0) " +
           "FROM CashbookTransaction t WHERE t.account.id = :accountId " +
           "AND t.transactionDate BETWEEN :from AND :to")
    BigDecimal sumDebitByAccountAndPeriod(@Param("accountId") Long accountId,
                                          @Param("from") LocalDate from,
                                          @Param("to") LocalDate to);
}
