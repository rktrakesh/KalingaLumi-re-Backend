package com.business.erp.expense.service;

import com.business.erp.common.response.PageResponse;
import com.business.erp.expense.dto.request.CreateExpenseRequest;
import com.business.erp.expense.dto.response.ExpenseResponse;
import com.business.erp.expense.entity.Expense;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;

public interface ExpenseService {
    ExpenseResponse create(CreateExpenseRequest request, String createdBy);

    ExpenseResponse approve(Long id, String approvedBy);

    ExpenseResponse cancel(Long id, String remarks, String cancelledBy);

    ExpenseResponse update(Long id, CreateExpenseRequest request);

    PageResponse<ExpenseResponse> search(Expense.ExpenseStatus status, Expense.ExpenseCategory category, LocalDate from, LocalDate to, Pageable pageable);

    ExpenseResponse findById(Long id);
}