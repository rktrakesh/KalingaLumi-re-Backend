package com.business.erp.expense.service.impl;

import com.business.erp.cashbook.service.CashbookService;
import com.business.erp.common.audit.AuditService;
import com.business.erp.common.exception.BusinessException;
import com.business.erp.common.exception.ResourceNotFoundException;
import com.business.erp.common.response.PageResponse;
import com.business.erp.common.sequence.ReferenceNumberService;
import com.business.erp.expense.dto.request.CreateExpenseRequest;
import com.business.erp.expense.dto.response.ExpenseResponse;
import com.business.erp.expense.entity.Expense;
import com.business.erp.expense.repository.ExpenseRepository;
import com.business.erp.expense.service.ExpenseService;
import com.business.erp.monthclosing.service.MonthClosingService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ExpenseServiceImpl implements ExpenseService {

    private final ExpenseRepository expenseRepository;
    private final ReferenceNumberService refService;
    private final MonthClosingService monthClosingService;
    private final CashbookService cashbookService;
    private final AuditService auditService;
    private final Logger log = LoggerFactory.getLogger(ExpenseServiceImpl.class);

    @Transactional
    public ExpenseResponse create(CreateExpenseRequest req, String createdBy) {
        log.info("ExpenseServiceImpl:method :: invoked");
        log.debug("ExpenseServiceImpl:create :: invoked");
        Expense exp = expenseRepository.save(Expense.builder()
                .expenseReference(refService.generateExpenseReference(req.getExpenseDate()))
                .expenseDate(req.getExpenseDate())
                .amount(req.getAmount())
                .category(req.getCategory())
                .remarks(req.getRemarks())
                .status(Expense.ExpenseStatus.DRAFT)
                .build());
        return toResponse(exp);
    }

    @Transactional
    public ExpenseResponse approve(Long id, String approvedBy) {
        log.info("ExpenseServiceImpl:method :: invoked");
        log.debug("ExpenseServiceImpl:approve :: invoked");
        Expense exp = getExpense(id);
        if (exp.getStatus() != Expense.ExpenseStatus.DRAFT)
            throw new BusinessException("Only DRAFT expenses can be approved");
        if (monthClosingService.isMonthClosed(exp.getExpenseDate().getYear(), exp.getExpenseDate().getMonthValue()))
            throw new BusinessException("Month is closed");

        exp.setStatus(Expense.ExpenseStatus.APPROVED);
        exp.setApprovedBy(approvedBy);
        exp.setApprovedDate(LocalDateTime.now());
        Expense saved = expenseRepository.save(exp);

        cashbookService.recordExpense(exp.getAmount(), exp.getExpenseReference(), exp.getCategory().name(), exp.getId());
        auditService.log("EXPENSE", "APPROVE", "Expense", id);
        return toResponse(saved);
    }

    @Transactional
    public ExpenseResponse cancel(Long id, String remarks, String cancelledBy) {
        log.info("ExpenseServiceImpl:method :: invoked");
        log.debug("ExpenseServiceImpl:cancel :: invoked");
        Expense exp = getExpense(id);
        if (exp.getStatus() == Expense.ExpenseStatus.CANCELLED)
            throw new BusinessException("Expense is already cancelled");
        Object oldVal = toResponse(exp);
        exp.setStatus(Expense.ExpenseStatus.CANCELLED);
        exp.setRemarks(remarks != null ? remarks : exp.getRemarks());
        Expense saved = expenseRepository.save(exp);
        auditService.log("EXPENSE", "CANCEL", "Expense", id, oldVal, toResponse(saved));
        return toResponse(saved);
    }

    @Transactional
    public ExpenseResponse update(Long id, CreateExpenseRequest req) {
        log.info("ExpenseServiceImpl:method :: invoked");
        log.debug("ExpenseServiceImpl:update :: invoked");
        Expense exp = getExpense(id);
        if (exp.getStatus() != Expense.ExpenseStatus.DRAFT)
            throw new BusinessException("Only DRAFT expenses can be edited");
        exp.setExpenseDate(req.getExpenseDate());
        exp.setAmount(req.getAmount());
        exp.setCategory(req.getCategory());
        exp.setRemarks(req.getRemarks());
        return toResponse(expenseRepository.save(exp));
    }

    public PageResponse<ExpenseResponse> search(Expense.ExpenseStatus status,
                                                Expense.ExpenseCategory category, LocalDate from, LocalDate to, Pageable pageable) {
        log.debug("ExpenseServiceImpl:search :: invoked");
        return PageResponse.of(expenseRepository.search(status, category, from, to, pageable).map(this::toResponse));
    }

    public ExpenseResponse findById(Long id) {
        log.debug("ExpenseServiceImpl:findById :: invoked");
        return toResponse(getExpense(id));
    }

    private Expense getExpense(Long id) {
        return expenseRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Expense", id));
    }

    private ExpenseResponse toResponse(Expense e) {
        return ExpenseResponse.builder()
                .id(e.getId()).expenseReference(e.getExpenseReference())
                .expenseDate(e.getExpenseDate()).amount(e.getAmount())
                .category(e.getCategory().name()).remarks(e.getRemarks())
                .status(e.getStatus().name()).approvedBy(e.getApprovedBy())
                .approvedDate(e.getApprovedDate()).createdBy(e.getCreatedBy())
                .createdDate(e.getCreatedDate()).build();
    }
}
