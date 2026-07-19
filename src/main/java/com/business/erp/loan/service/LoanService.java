package com.business.erp.loan.service;

import com.business.erp.common.response.PageResponse;
import com.business.erp.loan.dto.request.CreateLoanRequest;
import com.business.erp.loan.dto.response.LoanLedgerResponse;
import com.business.erp.loan.dto.response.LoanResponse;
import com.business.erp.loan.entity.EmployeeLoan;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface LoanService {
    LoanResponse create(CreateLoanRequest request, String createdBy);

    LoanResponse approve(Long id, String approvedBy);

    LoanResponse reject(Long id, String remarks, String rejectedBy);

    BigDecimal[] calculateAndPostDeductions(Long empId, LocalDate payrollMonth, BigDecimal availableSalary, String postedBy);

    BigDecimal getCurrentBalance(Long loanId);

    List<LoanLedgerResponse> getLedger(Long loanId);

    PageResponse<LoanResponse> search(Long empId, EmployeeLoan.LoanStatus status, Pageable pageable);

    LoanResponse getActiveLoan(Long empId);
}