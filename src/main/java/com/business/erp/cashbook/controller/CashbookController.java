package com.business.erp.cashbook.controller;

import com.business.erp.cashbook.dto.response.CashbookSummaryResponse;
import com.business.erp.cashbook.dto.response.CashbookTransactionResponse;
import com.business.erp.cashbook.dto.request.ManualCashbookRequest;
import com.business.erp.cashbook.entity.CashbookTransaction;
import com.business.erp.cashbook.service.CashbookService;
import com.business.erp.common.response.ApiResponse;
import com.business.erp.common.response.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/cashbook")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('ROLE_ADMIN')")
@Tag(name = "Cashbook", description = "Cashbook operations")
public class CashbookController {

    private final CashbookService cashbookService;

    @GetMapping
    @Operation(summary = "Get cashbook summary for a given month", description = "Returns total income, total expenses, and net balance for the specified month. Defaults to current month if not provided.")
    public ResponseEntity<ApiResponse<CashbookSummaryResponse>> getSummary(
            @RequestParam(defaultValue = "#{T(java.time.LocalDate).now().getYear()}") int year,
            @RequestParam(defaultValue = "#{T(java.time.LocalDate).now().getMonthValue()}") int month) {
        return ResponseEntity.ok(ApiResponse.ok(cashbookService.getSummary(year, month)));
    }

    @GetMapping("/transactions")
    @Operation(summary = "Search cashbook transactions", description = "Search transactions with optional filters: accountId, date range (from/to), and transaction type (INCOME/EXPENSE). Supports pagination.")
    public ResponseEntity<ApiResponse<PageResponse<CashbookTransactionResponse>>> getTransactions(
            @RequestParam(required = false) Long accountId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) CashbookTransaction.TransactionType type,
            @PageableDefault(size = 50) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.ok(cashbookService.search(accountId, from, to, type, pageable)));
    }

    @PostMapping("/manual-entry")
    @Operation(summary = "Create a manual cashbook entry", description = "Allows creation of a manual cashbook transaction. Requires accountId, amount, transaction type (INCOME/EXPENSE), and optional description.")
    public ResponseEntity<ApiResponse<CashbookTransactionResponse>> manualEntry(
            @Valid @RequestBody ManualCashbookRequest req,
            @AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(ApiResponse.ok(cashbookService.manualEntry(req, user.getUsername())));
    }
}
