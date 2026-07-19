package com.business.erp.payroll.controller;

import com.business.erp.common.response.ApiResponse;
import com.business.erp.payroll.dto.request.GeneratePayrollRequest;
import com.business.erp.payroll.dto.response.PayrollDetailResponse;
import com.business.erp.payroll.dto.response.PayrollRunResponse;
import com.business.erp.payroll.service.PayrollService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/payroll")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Payroll", description = "Payroll operations")
public class PayrollController {

    private final PayrollService payrollService;
    private final Logger log = LoggerFactory.getLogger(PayrollController.class);

    @PostMapping("/generate")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<ApiResponse<PayrollRunResponse>> generate(
            @Valid @RequestBody GeneratePayrollRequest req,
            @AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(ApiResponse.ok(
                payrollService.generate(req, user.getUsername()), "Payroll generated successfully"));
    }

    @PostMapping("/regenerate/{runId}")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<ApiResponse<PayrollRunResponse>> regenerate(
            @PathVariable Long runId,
            @AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(ApiResponse.ok(
                payrollService.regenerate(runId, user.getUsername()), "Payroll regenerated"));
    }

    @GetMapping
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<ApiResponse<List<PayrollRunResponse>>> getAll() {
        return ResponseEntity.ok(ApiResponse.ok(payrollService.getAllRuns()));
    }

    @GetMapping("/{runId}")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<ApiResponse<PayrollRunResponse>> getRun(@PathVariable Long runId) {
        return ResponseEntity.ok(ApiResponse.ok(payrollService.getRun(runId)));
    }

    @GetMapping("/{runId}/details")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<ApiResponse<List<PayrollDetailResponse>>> getDetails(@PathVariable Long runId) {
        return ResponseEntity.ok(ApiResponse.ok(payrollService.getDetails(runId)));
    }

    @GetMapping("/employee/{empId}")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN','ROLE_EMPLOYEE')")
    public ResponseEntity<ApiResponse<PayrollDetailResponse>> getPayslip(
            @PathVariable Long empId,
            @RequestParam int year,
            @RequestParam int month) {
        return ResponseEntity.ok(ApiResponse.ok(payrollService.getEmployeePayslip(empId, year, month)));
    }
}
