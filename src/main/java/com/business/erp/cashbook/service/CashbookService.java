package com.business.erp.cashbook.service;

import com.business.erp.cashbook.dto.request.ManualCashbookRequest;
import com.business.erp.cashbook.dto.response.CashbookSummaryResponse;
import com.business.erp.cashbook.dto.response.CashbookTransactionResponse;
import com.business.erp.cashbook.entity.CashbookTransaction;
import com.business.erp.common.response.PageResponse;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;

public interface CashbookService {
    void recordSalaryPayment(BigDecimal amount, String payrollRef, String empName);

    void recordLoanDisbursement(BigDecimal amount, String loanRef, String empName);

    void recordCustomerPayment(BigDecimal amount, String invoiceRef, String customerName, Long invoiceId);

    void recordSupplierPayment(BigDecimal amount, String purchaseRef, String supplierName, Long purchaseId);

    void recordExpense(BigDecimal amount, String expenseRef, String category, Long expenseId);

    CashbookTransactionResponse manualEntry(ManualCashbookRequest request, String createdBy);

    CashbookSummaryResponse getSummary(int year, int month);

    PageResponse<CashbookTransactionResponse> search(Long accountId, LocalDate from, LocalDate to, CashbookTransaction.TransactionType type, Pageable pageable);
}