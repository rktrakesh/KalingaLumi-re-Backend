package com.business.erp.loan.repository;

import com.business.erp.loan.entity.LoanLedger;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface LoanLedgerRepository extends JpaRepository<LoanLedger, Long> {
    List<LoanLedger> findByLoanIdOrderByTransactionDateAsc(Long loanId);

    @Query("SELECT MAX(ll.balanceAfter) FROM LoanLedger ll WHERE ll.loan.id = :loanId " +
            "AND ll.id = (SELECT MAX(l2.id) FROM LoanLedger l2 WHERE l2.loan.id = :loanId)")
    Optional<BigDecimal> findCurrentBalance(@Param("loanId") Long loanId);

    @Query("SELECT ll FROM LoanLedger ll WHERE ll.loan.id = :loanId ORDER BY ll.id DESC LIMIT 1")
    Optional<LoanLedger> findLatestEntry(@Param("loanId") Long loanId);
}
