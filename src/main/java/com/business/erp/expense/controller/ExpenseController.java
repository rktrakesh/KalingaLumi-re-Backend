package com.business.erp.expense.controller;

import com.business.erp.common.response.ApiResponse;
import com.business.erp.common.response.PageResponse;
import com.business.erp.expense.dto.request.CreateExpenseRequest;
import com.business.erp.expense.dto.response.ExpenseResponse;
import com.business.erp.expense.entity.Expense;
import com.business.erp.expense.service.ExpenseService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/expenses")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Expenses", description = "Expenses operations")
public class ExpenseController {

    private final ExpenseService expenseService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ROLE_ADMIN','ROLE_MANAGER')")
    @Tag(name = "Expenses", description = "Create a new expense report")
    public ResponseEntity<ApiResponse<ExpenseResponse>> create(
            @Valid @RequestBody CreateExpenseRequest req,
            @AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(expenseService.create(req, user.getUsername())));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ROLE_ADMIN','ROLE_MANAGER')")
    @Tag(name = "Expenses", description = "Search and filter expenses with pagination")
    public ResponseEntity<ApiResponse<PageResponse<ExpenseResponse>>> search(
            @RequestParam(required = false) Expense.ExpenseStatus status,
            @RequestParam(required = false) Expense.ExpenseCategory category,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.ok(expenseService.search(status, category, from, to, pageable)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN','ROLE_MANAGER')")
    @Tag(name = "Expenses", description = "Get expense details by ID")
    public ResponseEntity<ApiResponse<ExpenseResponse>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(expenseService.findById(id)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @Tag(name = "Expenses", description = "Update an existing expense report (only if pending)")
    public ResponseEntity<ApiResponse<ExpenseResponse>> update(
            @PathVariable Long id, @Valid @RequestBody CreateExpenseRequest req) {
        return ResponseEntity.ok(ApiResponse.ok(expenseService.update(id, req)));
    }

    @PutMapping("/{id}/approve")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @Tag(name = "Expenses", description = "Approve a pending expense report")
    public ResponseEntity<ApiResponse<ExpenseResponse>> approve(@PathVariable Long id,
                                                                @AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(ApiResponse.ok(expenseService.approve(id, user.getUsername()), "Expense approved"));
    }

    @PutMapping("/{id}/cancel")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @Tag(name = "Expenses", description = "Cancel a pending or approved expense report with optional remarks")
    public ResponseEntity<ApiResponse<ExpenseResponse>> cancel(@PathVariable Long id,
                                                               @RequestParam(required = false) String remarks,
                                                               @AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(ApiResponse.ok(expenseService.cancel(id, remarks, user.getUsername()), "Expense cancelled"));
    }
}
