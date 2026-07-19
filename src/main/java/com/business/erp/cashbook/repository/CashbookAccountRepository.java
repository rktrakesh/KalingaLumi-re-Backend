package com.business.erp.cashbook.repository;

import com.business.erp.cashbook.entity.CashbookAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CashbookAccountRepository extends JpaRepository<CashbookAccount, Long> {
    List<CashbookAccount> findByStatus(CashbookAccount.AccountStatus status);

    Optional<CashbookAccount> findFirstByAccountTypeAndStatus(
            CashbookAccount.AccountType type, CashbookAccount.AccountStatus status);
}
