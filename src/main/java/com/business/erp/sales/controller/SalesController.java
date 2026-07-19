package com.business.erp.sales.controller;

import com.business.erp.common.response.ApiResponse;
import com.business.erp.common.response.PageResponse;
import com.business.erp.sales.dto.request.CreateSaleRequest;
import com.business.erp.sales.dto.request.SalePaymentRequest;
import com.business.erp.sales.dto.response.SaleResponse;
import com.business.erp.sales.dto.request.SaleReturnRequest;
import com.business.erp.sales.entity.SalesInvoice;
import com.business.erp.sales.service.SalesService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
@RequestMapping("/api/v1/sales")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Sales", description = "Sales operations")
public class SalesController {

    private final SalesService salesService;
    private final Logger log = LoggerFactory.getLogger(SalesController.class);

    @PostMapping
    @PreAuthorize("hasAnyRole('ROLE_ADMIN','ROLE_MANAGER')")
    public ResponseEntity<ApiResponse<SaleResponse>> create(
            @Valid @RequestBody CreateSaleRequest req,
            @AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(salesService.create(req, user.getUsername())));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ROLE_ADMIN','ROLE_MANAGER')")
    public ResponseEntity<ApiResponse<PageResponse<SaleResponse>>> search(
            @RequestParam(required = false) Long customerId,
            @RequestParam(required = false) SalesInvoice.PaymentStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.ok(salesService.search(customerId, status, from, to, pageable)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN','ROLE_MANAGER')")
    public ResponseEntity<ApiResponse<SaleResponse>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(salesService.findById(id)));
    }

    @PostMapping("/{id}/payment")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN','ROLE_MANAGER')")
    public ResponseEntity<ApiResponse<SaleResponse>> recordPayment(
            @PathVariable Long id, @Valid @RequestBody SalePaymentRequest req,
            @AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(ApiResponse.ok(
                salesService.recordPayment(id, req, user.getUsername()), "Payment recorded"));
    }

    @PostMapping("/{id}/return")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<ApiResponse<SaleResponse>> processReturn(
            @PathVariable Long id, @Valid @RequestBody SaleReturnRequest req,
            @AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(ApiResponse.ok(
                salesService.processReturn(id, req, user.getUsername()), "Return processed"));
    }
}

