package com.business.erp.payroll.controller;

import com.business.erp.auth.service.AuthenticatedEmployeeAccessService;
import com.business.erp.common.response.ApiResponse;
import com.business.erp.payroll.dto.request.*;
import com.business.erp.payroll.dto.response.*;
import com.business.erp.payroll.service.PayrollService;
import io.swagger.v3.oas.annotations.Operation;
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
@Tag(name = "Payroll", description = "Payroll lifecycle, calculation engine, disbursement and payslip management")
public class PayrollController {

    private final PayrollService payrollService;
    private final AuthenticatedEmployeeAccessService employeeAccessService;
    private final Logger log = LoggerFactory.getLogger(PayrollController.class);

    // ── Lifecycle ────────────────────────────────────────────────────────
    @PostMapping("/generate")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @Operation(summary = "Generate payroll", description = "First-time calculation for a period. Captures the immutable settings snapshot.")
    public ResponseEntity<ApiResponse<PayrollRunResponse>> generate(
            @Valid @RequestBody GeneratePayrollRequest req,
            @AuthenticationPrincipal UserDetails user) {
        log.info("PayrollController:generate :: year={} month={} by={}", req.getYear(), req.getMonth(), user.getUsername());
        return ResponseEntity.ok(ApiResponse.ok(
                payrollService.generate(req, user.getUsername()), "Payroll generated successfully"));
    }

    @PostMapping("/{runId}/recalculate")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @Operation(summary = "Recalculate payroll", description = "Creates a new calculation version for a run that is not yet VERIFIED. Never overwrites the previous version.")
    public ResponseEntity<ApiResponse<PayrollRunResponse>> recalculate(
            @PathVariable Long runId, @AuthenticationPrincipal UserDetails user) {
        log.info("PayrollController:recalculate :: runId={} by={}", runId, user.getUsername());
        return ResponseEntity.ok(ApiResponse.ok(
                payrollService.recalculate(runId, user.getUsername()), "Payroll recalculated as a new version"));
    }

    @PostMapping("/{runId}/verify")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @Operation(summary = "Verify payroll", description = "Confirms the calculated numbers are correct. CALCULATED -> VERIFIED.")
    public ResponseEntity<ApiResponse<PayrollRunResponse>> verify(
            @PathVariable Long runId, @RequestBody(required = false) ActionRemarksRequest req,
            @AuthenticationPrincipal UserDetails user) {
        log.info("PayrollController:verify :: runId={} by={}", runId, user.getUsername());
        String remarks = req != null ? req.getRemarks() : null;
        return ResponseEntity.ok(ApiResponse.ok(
                payrollService.verify(runId, user.getUsername(), remarks), "Payroll verified"));
    }

    @PostMapping("/{runId}/approve")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @Operation(summary = "Approve payroll", description = "Approves for disbursement. VERIFIED -> APPROVED. Freezes attendance for the period.")
    public ResponseEntity<ApiResponse<PayrollRunResponse>> approve(
            @PathVariable Long runId, @RequestBody(required = false) ActionRemarksRequest req,
            @AuthenticationPrincipal UserDetails user) {
        log.info("PayrollController:approve :: runId={} by={}", runId, user.getUsername());
        String remarks = req != null ? req.getRemarks() : null;
        return ResponseEntity.ok(ApiResponse.ok(
                payrollService.approve(runId, user.getUsername(), remarks), "Payroll approved and attendance locked"));
    }

    @PostMapping("/{runId}/reopen")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @Operation(summary = "Reopen payroll", description = "Reopens a VERIFIED/APPROVED/PROCESSED run: unlocks attendance, reverses leave settlement, creates a new version.")
    public ResponseEntity<ApiResponse<PayrollRunResponse>> reopen(
            @PathVariable Long runId, @Valid @RequestBody ReopenPayrollRequest req,
            @AuthenticationPrincipal UserDetails user) {
        log.info("PayrollController:reopen :: runId={} by={} reason={}", runId, user.getUsername(), req.getReason());
        return ResponseEntity.ok(ApiResponse.ok(
                payrollService.reopen(runId, user.getUsername(), req.getReason()),
                "Payroll reopened — attendance unlocked, new version created"));
    }

    @PostMapping("/{runId}/lock")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @Operation(summary = "Lock payroll run", description = "Permanently locks a fully PAID run.")
    public ResponseEntity<ApiResponse<PayrollRunResponse>> lock(
            @PathVariable Long runId, @AuthenticationPrincipal UserDetails user) {
        log.info("PayrollController:lock :: runId={} by={}", runId, user.getUsername());
        return ResponseEntity.ok(ApiResponse.ok(
                payrollService.lockRun(runId, user.getUsername()), "Payroll run locked successfully"));
    }

    // ── Disbursement ─────────────────────────────────────────────────────
    @PostMapping("/details/{detailId}/disburse")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @Operation(summary = "Disburse salary — single employee")
    public ResponseEntity<ApiResponse<PayrollDetailResponse>> disburseOne(
            @PathVariable Long detailId, @Valid @RequestBody DisbursePaymentRequest req,
            @AuthenticationPrincipal UserDetails user) {
        log.info("PayrollController:disburseOne :: detailId={} mode={} by={}", detailId, req.getPaymentMode(), user.getUsername());
        return ResponseEntity.ok(ApiResponse.ok(
                payrollService.disburseOne(detailId, req, user.getUsername()), "Salary disbursed successfully"));
    }

    @PostMapping("/{runId}/disburse-all")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @Operation(summary = "Disburse all salaries")
    public ResponseEntity<ApiResponse<Void>> disburseAll(
            @PathVariable Long runId, @Valid @RequestBody DisbursePaymentRequest req,
            @AuthenticationPrincipal UserDetails user) {
        log.info("PayrollController:disburseAll :: runId={} mode={} by={}", runId, req.getPaymentMode(), user.getUsername());
        payrollService.disburseAll(runId, req, user.getUsername());
        return ResponseEntity.ok(ApiResponse.ok("All pending salaries disbursed successfully"));
    }

    // ── Reads ────────────────────────────────────────────────────────────
    @GetMapping
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @Operation(summary = "List current payroll runs", description = "Latest version of every period, newest first")
    public ResponseEntity<ApiResponse<List<PayrollRunResponse>>> getAll() {
        return ResponseEntity.ok(ApiResponse.ok(payrollService.getAllRuns()));
    }

    @GetMapping("/{runId}")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @Operation(summary = "Get payroll run by ID")
    public ResponseEntity<ApiResponse<PayrollRunResponse>> getRun(@PathVariable Long runId) {
        return ResponseEntity.ok(ApiResponse.ok(payrollService.getRun(runId)));
    }

    @GetMapping("/history")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @Operation(summary = "Version history for a period", description = "Every calculation version ever generated, oldest first")
    public ResponseEntity<ApiResponse<List<PayrollRunResponse>>> getVersionHistory(
            @RequestParam int year, @RequestParam int month) {
        return ResponseEntity.ok(ApiResponse.ok(payrollService.getVersionHistory(year, month)));
    }

    @GetMapping("/{runId}/details")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @Operation(summary = "Get payroll details", description = "All employee payslips for a payroll run")
    public ResponseEntity<ApiResponse<List<PayrollDetailResponse>>> getDetails(@PathVariable Long runId) {
        return ResponseEntity.ok(ApiResponse.ok(payrollService.getDetails(runId)));
    }

    @GetMapping("/employee/{empId}")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN','ROLE_EMPLOYEE')")
    @Operation(summary = "Get employee payslip", description = "Fetch individual payslip for a specific month (current version)")
    public ResponseEntity<ApiResponse<PayrollDetailResponse>> getPayslip(
            @PathVariable Long empId,
            @RequestParam int year,
            @RequestParam int month,
            @AuthenticationPrincipal UserDetails user) {
        Long authorizedEmployeeId = employeeAccessService.requireAdminOrSelf(user, empId);
        return ResponseEntity.ok(ApiResponse.ok(
                payrollService.getEmployeePayslip(authorizedEmployeeId, year, month)));
    }

    @GetMapping("/{runId}/calculation-logs")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @Operation(summary = "Calculation logs for a run", description = "Full per-employee calculation trail for dispute resolution")
    public ResponseEntity<ApiResponse<List<PayrollCalculationLogResponse>>> getCalculationLogs(@PathVariable Long runId) {
        return ResponseEntity.ok(ApiResponse.ok(payrollService.getCalculationLogs(runId)));
    }

    @GetMapping("/employee/{empId}/calculation-history")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN','ROLE_EMPLOYEE')")
    @Operation(summary = "Employee calculation history", description = "Every calculation ever run for this employee, across all periods and versions")
    public ResponseEntity<ApiResponse<List<PayrollCalculationLogResponse>>> getEmployeeCalculationHistory(
            @PathVariable Long empId,
            @AuthenticationPrincipal UserDetails user) {
        Long authorizedEmployeeId = employeeAccessService.requireAdminOrSelf(user, empId);
        return ResponseEntity.ok(ApiResponse.ok(
                payrollService.getEmployeeCalculationHistory(authorizedEmployeeId)));
    }

    @GetMapping("/{runId}/dashboard")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @Operation(summary = "Payroll dashboard summary", description = "Totals by pay component for a run")
    public ResponseEntity<ApiResponse<PayrollDashboardResponse>> getDashboard(@PathVariable Long runId) {
        return ResponseEntity.ok(ApiResponse.ok(payrollService.getDashboard(runId)));
    }

    @GetMapping("/exceptions")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @Operation(summary = "Payroll exception report", description = "Attendance/OT/leave/holiday anomalies for a period, before approval")
    public ResponseEntity<ApiResponse<List<PayrollExceptionResponse>>> getExceptionReport(
            @RequestParam int year, @RequestParam int month) {
        return ResponseEntity.ok(ApiResponse.ok(payrollService.getExceptionReport(year, month)));
    }
}
