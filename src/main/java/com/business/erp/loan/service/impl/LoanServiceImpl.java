package com.business.erp.loan.service.impl;

import com.business.erp.cashbook.service.CashbookService;
import com.business.erp.common.audit.AuditService;
import com.business.erp.common.exception.BusinessException;
import com.business.erp.common.exception.ResourceNotFoundException;
import com.business.erp.common.response.PageResponse;
import com.business.erp.common.sequence.ReferenceNumberService;
import com.business.erp.employee.service.EmployeeService;
import com.business.erp.loan.dto.request.CreateLoanRequest;
import com.business.erp.loan.dto.response.LoanLedgerResponse;
import com.business.erp.loan.dto.response.LoanResponse;
import com.business.erp.loan.entity.EmployeeLoan;
import com.business.erp.loan.entity.LoanLedger;
import com.business.erp.loan.repository.LoanLedgerRepository;
import com.business.erp.loan.repository.LoanRepository;
import com.business.erp.loan.service.LoanService;
import com.business.erp.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class LoanServiceImpl implements LoanService {

    private final LoanRepository loanRepository;
    private final LoanLedgerRepository loanLedgerRepository;
    private final EmployeeService employeeService;
    private final ReferenceNumberService refService;
    private final NotificationService notificationService;
    private final AuditService auditService;
    private final CashbookService cashbookService;
    private final Logger log =  LoggerFactory.getLogger(LoanServiceImpl.class);

    @Transactional
    public LoanResponse create(CreateLoanRequest req, String createdBy) {
        log.debug("Creating loan for employeeId={}, principal={}, rate={}", req.getEmployeeId(),
                req.getPrincipalAmount(), req.getInterestRate());

        if (loanRepository.existsByEmployeeIdAndStatus(req.getEmployeeId(), EmployeeLoan.LoanStatus.ACTIVE)) {
            log.warn("Failed to create loan for employeeId={}: employee already has active loan", req.getEmployeeId());
            throw new BusinessException("Employee already has an active loan. Cannot create another.");
        }

        // monthly interest = principal * rate / 100 (simple interest, monthly)
        BigDecimal monthlyInterest = req.getPrincipalAmount()
                .multiply(req.getInterestRate())
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

        EmployeeLoan loan = loanRepository.save(EmployeeLoan.builder()
                .loanReference(refService.generateLoanReference())
                .employee(employeeService.getEmployee(req.getEmployeeId()))
                .principalAmount(req.getPrincipalAmount())
                .interestRate(req.getInterestRate())
                .monthlyInterest(monthlyInterest)
                .monthlyPrincipalPayment(req.getMonthlyPrincipalPayment())
                .status(EmployeeLoan.LoanStatus.PENDING_APPROVAL)
                .remarks(req.getRemarks())
                .build());

        log.info("Loan created successfully: id={}, reference={}, employeeId={}, status=PENDING_APPROVAL",
                loan.getId(), loan.getLoanReference(), loan.getEmployee().getId());

        notificationService.createLoanApprovalNotification(loan.getEmployee(), loan.getId());
        return toResponse(loan, null);
    }

    @Transactional
    public LoanResponse approve(Long id, String approvedBy) {
        log.debug("Approving loan: id={}, approvedBy={}", id, approvedBy);

        EmployeeLoan loan = getLoan(id);
        if (loan.getStatus() != EmployeeLoan.LoanStatus.PENDING_APPROVAL) {
            log.warn("Cannot approve loan id={}: status is {} instead of PENDING_APPROVAL", id, loan.getStatus());
            throw new BusinessException("Only PENDING_APPROVAL loans can be approved");
        }
        if (loanRepository.existsByEmployeeIdAndStatus(loan.getEmployee().getId(), EmployeeLoan.LoanStatus.ACTIVE)) {
            log.warn("Cannot approve loan id={}: employee already has active loan", id);
            throw new BusinessException("Employee already has an active loan");
        }

        loan.setStatus(EmployeeLoan.LoanStatus.ACTIVE);
        loan.setApprovedBy(approvedBy);
        loan.setApprovedDate(LocalDateTime.now());
        loan.setDisbursementDate(LocalDate.now());
        loanRepository.save(loan);

        log.info("Loan approved and activated: id={}, reference={}, employeeId={}, amount={}",
                id, loan.getLoanReference(), loan.getEmployee().getId(), loan.getPrincipalAmount());

        // Record LOAN_ISSUED in ledger
        appendLedger(loan, LoanLedger.LoanTransactionType.LOAN_ISSUED,
                loan.getPrincipalAmount(), null, "Loan disbursed", approvedBy);

        // Record in cashbook
        log.debug("Recording loan disbursement in cashbook for loanReference={}", loan.getLoanReference());
        cashbookService.recordLoanDisbursement(loan.getPrincipalAmount(),
                loan.getLoanReference(), loan.getEmployee().getName());

        auditService.log("LOAN", "APPROVE", "EmployeeLoan", id);
        return toResponse(loan, getCurrentBalance(loan.getId()));
    }

    @Transactional
    public LoanResponse reject(Long id, String remarks, String rejectedBy) {
        log.debug("Rejecting loan: id={}, rejectedBy={}, remarks={}", id, rejectedBy, remarks);

        EmployeeLoan loan = getLoan(id);
        if (loan.getStatus() != EmployeeLoan.LoanStatus.PENDING_APPROVAL) {
            log.warn("Cannot reject loan id={}: status is {} instead of PENDING_APPROVAL", id, loan.getStatus());
            throw new BusinessException("Only PENDING_APPROVAL loans can be rejected");
        }

        loan.setStatus(EmployeeLoan.LoanStatus.REJECTED);
        loan.setRemarks(remarks);
        loan.setApprovedBy(rejectedBy);
        loan.setApprovedDate(LocalDateTime.now());

        loanRepository.save(loan);
        log.info("Loan rejected: id={}, reference={}, employeeId={}",
                id, loan.getLoanReference(), loan.getEmployee().getId());

        auditService.log("LOAN", "REJECT", "EmployeeLoan", id);
        return toResponse(loanRepository.save(loan), null);
    }

    /**
     * Called during payroll generation for each employee.
     * Returns {interest, principal} deductions and posts ledger entries.
     */
    @Transactional
    public BigDecimal[] calculateAndPostDeductions(Long empId, LocalDate payrollMonth,
                                                   BigDecimal availableSalary, String postedBy) {
        log.debug("Calculating loan deductions for employeeId={}, month={}", empId, payrollMonth);

        return loanRepository.findByEmployeeIdAndStatus(empId, EmployeeLoan.LoanStatus.ACTIVE)
                .map(loan -> {
                    BigDecimal currentBal = getCurrentBalance(loan.getId());
                    log.debug("Loan id={}, currentBalance={}", loan.getId(), currentBal);

                    if (currentBal.compareTo(BigDecimal.ZERO) <= 0) {
                        log.info("Closing loan id={}: balance reached zero", loan.getId());
                        closeLoanIfZero(loan, postedBy);
                        return new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO};
                    }

                    // Recalculate interest on current balance (simple interest)
                    BigDecimal interest = currentBal
                            .multiply(loan.getInterestRate())
                            .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

                    BigDecimal principal = loan.getMonthlyPrincipalPayment().min(currentBal);

                    // Cap total so salary never goes negative
                    BigDecimal totalDeduction = interest.add(principal);
                    if (totalDeduction.compareTo(availableSalary) > 0) {
                        log.debug("Capping deduction: total={} exceeds availableSalary={}", totalDeduction, availableSalary);
                        totalDeduction = availableSalary.max(BigDecimal.ZERO);
                        interest = totalDeduction.multiply(interest)
                                .divide(interest.add(principal), 2, RoundingMode.HALF_UP);
                        principal = totalDeduction.subtract(interest);
                    }

                    log.debug("Loan deductions for empId={}: interest={}, principal={}", empId, interest, principal);

                    // Post interest
                    appendLedger(loan, LoanLedger.LoanTransactionType.INTEREST_ADDED,
                            interest, null, "Monthly interest for " + payrollMonth, postedBy);

                    // Post salary deduction
                    BigDecimal deductionAmount = interest.add(principal).negate();
                    appendLedger(loan, LoanLedger.LoanTransactionType.SALARY_DEDUCTION,
                            deductionAmount, null, "Salary deduction " + payrollMonth, postedBy);

                    BigDecimal newBal = getCurrentBalance(loan.getId());
                    if (newBal.compareTo(BigDecimal.ZERO) <= 0) {
                        log.info("Closing loan id={}: balance reached zero after deductions", loan.getId());
                        closeLoanIfZero(loan, postedBy);
                    }

                    return new BigDecimal[]{interest, principal};
                })
                .orElse(new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO});
    }

    public BigDecimal getCurrentBalance(Long loanId) {
        BigDecimal balance = loanLedgerRepository.findLatestEntry(loanId)
                .map(LoanLedger::getBalanceAfter)
                .orElse(BigDecimal.ZERO);
        log.trace("Current balance for loanId={}: {}", loanId, balance);
        return balance;
    }

    public List<LoanLedgerResponse> getLedger(Long loanId) {
        log.debug("Fetching ledger for loanId={}", loanId);
        getLoan(loanId);  // Validate loan exists
        return loanLedgerRepository.findByLoanIdOrderByTransactionDateAsc(loanId)
                .stream().map(this::toLedgerResponse).toList();
    }

    @Transactional(readOnly = true)
    public PageResponse<LoanResponse> search(Long empId,
                                             EmployeeLoan.LoanStatus status,
                                             Pageable pageable) {
        log.debug("Searching loans for empId={}, status={}, page={}", empId, status, pageable.getPageNumber());

        Page<EmployeeLoan> page = loanRepository.search(empId, status, pageable);

        log.debug("Found {} loans matching criteria", page.getTotalElements());

        return PageResponse.of(
                page.map(l -> toResponse(
                        l,
                        l.getStatus() == EmployeeLoan.LoanStatus.ACTIVE
                                ? getCurrentBalance(l.getId())
                                : null
                ))
        );
    }

    @Transactional(readOnly = true)
    public LoanResponse getActiveLoan(Long empId) {
        log.debug("Fetching active loan for empId={}", empId);
        return loanRepository.findByEmployeeIdAndStatus(empId, EmployeeLoan.LoanStatus.ACTIVE)
                .map(l -> {
                    log.debug("Active loan found: id={}, reference={}", l.getId(), l.getLoanReference());
                    return toResponse(l, getCurrentBalance(l.getId()));
                })
                .orElseThrow(() -> {
                    log.warn("No active loan found for empId={}", empId);
                    return new ResourceNotFoundException("No active loan for employee: " + empId);
                });
    }

    private void appendLedger(EmployeeLoan loan, LoanLedger.LoanTransactionType type,
                              BigDecimal amount, Long refId, String remarks, String createdBy) {
        BigDecimal prevBal = getCurrentBalance(loan.getId());
        BigDecimal newBal = prevBal.add(amount);
        log.trace("Appending ledger entry for loanId={}, type={}, amount={}, prevBal={}, newBal={}",
                loan.getId(), type, amount, prevBal, newBal);

        loanLedgerRepository.save(LoanLedger.builder()
                .loan(loan).transactionDate(LocalDate.now()).transactionType(type)
                .amount(amount).balanceAfter(newBal.max(BigDecimal.ZERO))
                .referenceId(refId).remarks(remarks).createdBy(createdBy).build());
    }

    private void closeLoanIfZero(EmployeeLoan loan, String closedBy) {
        log.info("Closing loan: id={}, reference={}, closedBy={}", loan.getId(), loan.getLoanReference(), closedBy);
        loan.setStatus(EmployeeLoan.LoanStatus.CLOSED);
        loanRepository.save(loan);
    }

    private EmployeeLoan getLoan(Long id) {
        return loanRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("Loan not found: id={}", id);
                    return new ResourceNotFoundException("Loan", id);
                });
    }

    private LoanResponse toResponse(EmployeeLoan l, BigDecimal currentBalance) {
        return LoanResponse.builder()
                .id(l.getId()).loanReference(l.getLoanReference())
                .employeeId(l.getEmployee().getId())
                .employeeCode(l.getEmployee().getEmployeeCode())
                .employeeName(l.getEmployee().getName())
                .principalAmount(l.getPrincipalAmount())
                .interestRate(l.getInterestRate())
                .monthlyInterest(l.getMonthlyInterest())
                .monthlyPrincipalPayment(l.getMonthlyPrincipalPayment())
                .currentBalance(currentBalance)
                .status(l.getStatus().name())
                .disbursementDate(l.getDisbursementDate())
                .approvedBy(l.getApprovedBy())
                .approvedDate(l.getApprovedDate())
                .remarks(l.getRemarks())
                .createdDate(l.getCreatedDate()).build();
    }

    private LoanLedgerResponse toLedgerResponse(LoanLedger ll) {
        return LoanLedgerResponse.builder()
                .id(ll.getId()).transactionDate(ll.getTransactionDate())
                .transactionType(ll.getTransactionType().name())
                .amount(ll.getAmount()).balanceAfter(ll.getBalanceAfter())
                .remarks(ll.getRemarks()).createdBy(ll.getCreatedBy()).build();
    }
}
