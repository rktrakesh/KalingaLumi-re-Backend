package com.business.erp.loan.controller;

import com.business.erp.auth.service.AuthenticatedEmployeeAccessService;
import com.business.erp.common.response.ApiResponse;
import com.business.erp.common.response.PageResponse;
import com.business.erp.loan.dto.request.CreateLoanRequest;
import com.business.erp.loan.dto.response.LoanLedgerResponse;
import com.business.erp.loan.dto.response.LoanResponse;
import com.business.erp.loan.entity.EmployeeLoan;
import com.business.erp.loan.service.LoanService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/loans")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Loans", description = "Employee loan management")
public class LoanController {

    private final LoanService loanService;
    private final AuthenticatedEmployeeAccessService employeeAccessService;
    private final Logger log = LoggerFactory.getLogger(LoanController.class);

    @PostMapping
    @PreAuthorize("hasAnyRole('ROLE_ADMIN','ROLE_EMPLOYEE')")
    public ResponseEntity<ApiResponse<LoanResponse>> create(
            @Valid @RequestBody CreateLoanRequest req,
            @AuthenticationPrincipal UserDetails user) {
        employeeAccessService.requireAdminOrSelf(user, req.getEmployeeId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(loanService.create(req, user.getUsername()), "Loan request created"));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ROLE_ADMIN','ROLE_EMPLOYEE')")
    public ResponseEntity<ApiResponse<PageResponse<LoanResponse>>> getAll(
            @RequestParam(required = false) Long employeeId,
            @RequestParam(required = false) EmployeeLoan.LoanStatus status,
            @PageableDefault(size = 20) Pageable pageable,
            @AuthenticationPrincipal UserDetails user) {
        Long authorizedEmployeeId = employeeAccessService.requireAdminOrSelf(user, employeeId);
        return ResponseEntity.ok(ApiResponse.ok(loanService.search(authorizedEmployeeId, status, pageable)));
    }

    @PutMapping("/{id}/approve")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<ApiResponse<LoanResponse>> approve(
            @PathVariable Long id, @AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(ApiResponse.ok(loanService.approve(id, user.getUsername()), "Loan approved"));
    }

    @PutMapping("/{id}/reject")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<ApiResponse<LoanResponse>> reject(
            @PathVariable Long id,
            @RequestParam(required = false) String remarks,
            @AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(ApiResponse.ok(loanService.reject(id, remarks, user.getUsername()), "Loan rejected"));
    }

    @GetMapping("/{id}/ledger")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<ApiResponse<List<LoanLedgerResponse>>> getLedger(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(loanService.getLedger(id)));
    }

    @GetMapping("/employee/{empId}/active")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN','ROLE_EMPLOYEE')")
    public ResponseEntity<ApiResponse<LoanResponse>> getActiveLoan(
            @PathVariable Long empId,
            @AuthenticationPrincipal UserDetails user) {
        Long authorizedEmployeeId = employeeAccessService.requireAdminOrSelf(user, empId);
        return ResponseEntity.ok(ApiResponse.ok(loanService.getActiveLoan(authorizedEmployeeId)));
    }
}
