package com.business.erp.cashbook.service.impl;

import com.business.erp.cashbook.dto.request.ManualCashbookRequest;
import com.business.erp.cashbook.dto.response.CashbookSummaryResponse;
import com.business.erp.cashbook.dto.response.CashbookTransactionResponse;
import com.business.erp.cashbook.entity.CashbookAccount;
import com.business.erp.cashbook.entity.CashbookTransaction;
import com.business.erp.cashbook.repository.CashbookAccountRepository;
import com.business.erp.cashbook.repository.CashbookTransactionRepository;
import com.business.erp.cashbook.service.CashbookService;
import com.business.erp.common.exception.ResourceNotFoundException;
import com.business.erp.common.response.PageResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CashbookServiceImpl implements CashbookService {

    private final CashbookAccountRepository accountRepository;
    private final CashbookTransactionRepository txRepository;
    private final Logger log = LoggerFactory.getLogger(CashbookServiceImpl.class);

    @Transactional
    public void recordSalaryPayment(BigDecimal amount, String payrollRef, String empName) {
        log.info("CashbookServiceImpl:method :: invoked");
        log.debug("CashbookServiceImpl:recordSalaryPayment :: invoked");
        postToDefaultAccount(CashbookAccount.AccountType.CASH,
                CashbookTransaction.TransactionType.SALARY_PAYMENT,
                CashbookTransaction.FlowType.DEBIT, amount,
                "PAYROLL", null, "Salary: " + empName + " [" + payrollRef + "]");
    }

    @Transactional
    public void recordLoanDisbursement(BigDecimal amount, String loanRef, String empName) {
        log.info("CashbookServiceImpl:method :: invoked");
        log.debug("CashbookServiceImpl:recordLoanDisbursement :: invoked");
        postToDefaultAccount(CashbookAccount.AccountType.CASH,
                CashbookTransaction.TransactionType.LOAN_DISBURSEMENT,
                CashbookTransaction.FlowType.DEBIT, amount,
                "LOAN", null, "Loan disbursement: " + empName + " [" + loanRef + "]");
    }

    @Transactional
    public void recordCustomerPayment(BigDecimal amount, String invoiceRef, String customerName, Long invoiceId) {
        log.info("CashbookServiceImpl:method :: invoked");
        log.debug("CashbookServiceImpl:recordCustomerPayment :: invoked");
        postToDefaultAccount(CashbookAccount.AccountType.CASH,
                CashbookTransaction.TransactionType.CUSTOMER_PAYMENT,
                CashbookTransaction.FlowType.CREDIT, amount,
                "SALE_INVOICE", invoiceId, "Payment from: " + customerName + " [" + invoiceRef + "]");
    }

    @Transactional
    public void recordSupplierPayment(BigDecimal amount, String purchaseRef, String supplierName, Long purchaseId) {
        log.info("CashbookServiceImpl:method :: invoked");
        log.debug("CashbookServiceImpl:recordSupplierPayment :: invoked");
        postToDefaultAccount(CashbookAccount.AccountType.CASH,
                CashbookTransaction.TransactionType.SUPPLIER_PAYMENT,
                CashbookTransaction.FlowType.DEBIT, amount,
                "PURCHASE", purchaseId, "Payment to: " + supplierName + " [" + purchaseRef + "]");
    }

    @Transactional
    public void recordExpense(BigDecimal amount, String expenseRef, String category, Long expenseId) {
        log.info("CashbookServiceImpl:method :: invoked");
        log.debug("CashbookServiceImpl:recordExpense :: invoked");
        postToDefaultAccount(CashbookAccount.AccountType.CASH,
                CashbookTransaction.TransactionType.EXPENSE,
                CashbookTransaction.FlowType.DEBIT, amount,
                "EXPENSE", expenseId, "Expense [" + category + "]: " + expenseRef);
    }

    @Transactional
    public CashbookTransactionResponse manualEntry(ManualCashbookRequest req, String createdBy) {
        log.info("CashbookServiceImpl:method :: invoked");
        log.debug("CashbookServiceImpl:manualEntry :: invoked");
        CashbookAccount account = accountRepository.findById(req.getAccountId())
                .orElseThrow(() -> new ResourceNotFoundException("CashbookAccount", req.getAccountId()));
        CashbookTransaction tx = post(account, req.getTransactionDate(), req.getTransactionType(),
                req.getFlowType(), req.getAmount(), null, null, req.getDescription(), createdBy);
        return toTxResponse(tx);
    }

    public CashbookSummaryResponse getSummary(int year, int month) {
        log.debug("CashbookServiceImpl:getSummary :: invoked");
        LocalDate from = LocalDate.of(year, month, 1);
        LocalDate to = from.withDayOfMonth(from.lengthOfMonth());

        BigDecimal cashBal = getBalanceForType(CashbookAccount.AccountType.CASH);
        BigDecimal bankBal = getBalanceForType(CashbookAccount.AccountType.BANK);

        // Sum all credits and debits for month across all accounts
        BigDecimal income = txRepository.findAll().stream()
                .filter(t -> !t.getTransactionDate().isBefore(from) && !t.getTransactionDate().isAfter(to))
                .filter(t -> t.getFlowType() == CashbookTransaction.FlowType.CREDIT)
                .map(CashbookTransaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal expense = txRepository.findAll().stream()
                .filter(t -> !t.getTransactionDate().isBefore(from) && !t.getTransactionDate().isAfter(to))
                .filter(t -> t.getFlowType() == CashbookTransaction.FlowType.DEBIT)
                .map(CashbookTransaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return CashbookSummaryResponse.builder()
                .cashInHand(cashBal).bankBalance(bankBal)
                .totalBalance(cashBal.add(bankBal))
                .monthlyIncome(income).monthlyExpense(expense)
                .monthlyNet(income.subtract(expense)).build();
    }

    @Transactional(readOnly = true)
    public PageResponse<CashbookTransactionResponse> search(Long accountId, LocalDate from,
                                                            LocalDate to, CashbookTransaction.TransactionType type, Pageable pageable) {
        log.debug("CashbookServiceImpl:search :: invoked");
        return PageResponse.of(txRepository.search(accountId, from, to, type, pageable)
                .map(this::toTxResponse));
    }

    public List<CashbookAccount> getAccounts() {
        log.debug("CashbookServiceImpl:getAccounts :: invoked");
        return accountRepository.findByStatus(CashbookAccount.AccountStatus.ACTIVE);
    }

    private BigDecimal getBalanceForType(CashbookAccount.AccountType type) {
        return accountRepository.findFirstByAccountTypeAndStatus(type, CashbookAccount.AccountStatus.ACTIVE)
                .map(acc -> txRepository.findCurrentBalance(acc.getId())
                        .orElse(acc.getOpeningBalance()))
                .orElse(BigDecimal.ZERO);
    }

    private void postToDefaultAccount(CashbookAccount.AccountType type,
                                      CashbookTransaction.TransactionType txType,
                                      CashbookTransaction.FlowType flow,
                                      BigDecimal amount, String refType, Long refId,
                                      String description) {
        String user = getCurrentUser();
        accountRepository.findFirstByAccountTypeAndStatus(type, CashbookAccount.AccountStatus.ACTIVE)
                .ifPresent(acc -> post(acc, LocalDate.now(), txType, flow, amount, refType, refId, description, user));
    }

    private CashbookTransaction post(CashbookAccount account, LocalDate date,
                                     CashbookTransaction.TransactionType type,
                                     CashbookTransaction.FlowType flow, BigDecimal amount,
                                     String refType, Long refId, String description, String createdBy) {
        BigDecimal prevBal = txRepository.findCurrentBalance(account.getId())
                .orElse(account.getOpeningBalance());
        BigDecimal newBal = flow == CashbookTransaction.FlowType.CREDIT
                ? prevBal.add(amount) : prevBal.subtract(amount);

        return txRepository.save(CashbookTransaction.builder()
                .account(account).transactionDate(date).transactionType(type)
                .flowType(flow).amount(amount).balanceAfter(newBal)
                .referenceType(refType).referenceId(refId)
                .description(description).createdBy(createdBy).build());
    }

    private String getCurrentUser() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        return (auth != null) ? auth.getName() : "SYSTEM";
    }

    private CashbookTransactionResponse toTxResponse(CashbookTransaction t) {
        return CashbookTransactionResponse.builder()
                .id(t.getId()).accountId(t.getAccount().getId())
                .accountName(t.getAccount().getAccountName())
                .transactionDate(t.getTransactionDate())
                .transactionType(t.getTransactionType().name())
                .flowType(t.getFlowType().name())
                .amount(t.getAmount()).balanceAfter(t.getBalanceAfter())
                .referenceType(t.getReferenceType()).referenceId(t.getReferenceId())
                .description(t.getDescription()).createdBy(t.getCreatedBy())
                .createdDate(t.getCreatedDate()).build();
    }
}
