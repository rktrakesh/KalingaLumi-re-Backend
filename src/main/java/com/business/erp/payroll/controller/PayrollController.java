package com.business.erp.payroll.controller;

import com.business.erp.common.response.ApiResponse;
import com.business.erp.payroll.dto.request.DisbursePaymentRequest;
import com.business.erp.payroll.dto.request.GeneratePayrollRequest;
import com.business.erp.payroll.dto.response.PayrollDetailResponse;
import com.business.erp.payroll.dto.response.PayrollRunResponse;
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
@Tag(name = "Payroll", description = "Payroll generation, salary disbursement and payslip management")
public class PayrollController {

    private final PayrollService payrollService;
    private final Logger log = LoggerFactory.getLogger(PayrollController.class);

    @PostMapping("/generate")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @Operation(summary = "Generate payroll", description = "Calculate salaries for all active employees for the given month")
    public ResponseEntity<ApiResponse<PayrollRunResponse>> generate(
            @Valid @RequestBody GeneratePayrollRequest req,
            @AuthenticationPrincipal UserDetails user) {
        log.info("PayrollController:generate :: year={} month={} by={}", req.getYear(), req.getMonth(), user.getUsername());
        return ResponseEntity.ok(ApiResponse.ok(
                payrollService.generate(req, user.getUsername()), "Payroll generated successfully"));
    }

    @PostMapping("/regenerate/{runId}")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @Operation(summary = "Regenerate payroll", description = "Recalculate payroll for a run. Blocked if any salary is already PAID.")
    public ResponseEntity<ApiResponse<PayrollRunResponse>> regenerate(
            @PathVariable Long runId,
            @AuthenticationPrincipal UserDetails user) {
        log.info("PayrollController:regenerate :: runId={} by={}", runId, user.getUsername());
        return ResponseEntity.ok(ApiResponse.ok(
                payrollService.regenerate(runId, user.getUsername()), "Payroll regenerated"));
    }

    @PostMapping("/details/{detailId}/disburse")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @Operation(summary = "Disburse salary — single employee",
            description = "Mark one employee's salary as PAID and auto-post cashbook debit entry")
    public ResponseEntity<ApiResponse<PayrollDetailResponse>> disburseOne(
            @PathVariable Long detailId,
            @Valid @RequestBody DisbursePaymentRequest req,
            @AuthenticationPrincipal UserDetails user) {
        log.info("PayrollController:disburseOne :: detailId={} mode={} by={}", detailId, req.getPaymentMode(), user.getUsername());
        return ResponseEntity.ok(ApiResponse.ok(
                payrollService.disburseOne(detailId, req, user.getUsername()),
                "Salary disbursed successfully"));
    }

    @PostMapping("/{runId}/disburse-all")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @Operation(summary = "Disburse all salaries",
            description = "Pay all PENDING employees in this payroll run in one action")
    public ResponseEntity<ApiResponse<Void>> disburseAll(
            @PathVariable Long runId,
            @Valid @RequestBody DisbursePaymentRequest req,
            @AuthenticationPrincipal UserDetails user) {
        log.info("PayrollController:disburseAll :: runId={} mode={} by={}", runId, req.getPaymentMode(), user.getUsername());
        payrollService.disburseAll(runId, req, user.getUsername());
        return ResponseEntity.ok(ApiResponse.ok("All pending salaries disbursed successfully"));
    }

    @PostMapping("/{runId}/lock")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @Operation(summary = "Lock payroll run",
            description = "Lock a fully-paid payroll run to prevent any further modifications")
    public ResponseEntity<ApiResponse<PayrollRunResponse>> lock(
            @PathVariable Long runId,
            @AuthenticationPrincipal UserDetails user) {
        log.info("PayrollController:lock :: runId={} by={}", runId, user.getUsername());
        return ResponseEntity.ok(ApiResponse.ok(
                payrollService.lockRun(runId, user.getUsername()),
                "Payroll run locked successfully"));
    }

    @GetMapping
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @Operation(summary = "List all payroll runs", description = "Returns all runs ordered latest first")
    public ResponseEntity<ApiResponse<List<PayrollRunResponse>>> getAll() {
        log.debug("PayrollController:getAll :: fetching all runs");
        return ResponseEntity.ok(ApiResponse.ok(payrollService.getAllRuns()));
    }

    @GetMapping("/{runId}")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @Operation(summary = "Get payroll run by ID")
    public ResponseEntity<ApiResponse<PayrollRunResponse>> getRun(@PathVariable Long runId) {
        log.debug("PayrollController:getRun :: runId={}", runId);
        return ResponseEntity.ok(ApiResponse.ok(payrollService.getRun(runId)));
    }

    @GetMapping("/{runId}/details")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @Operation(summary = "Get payroll details", description = "All employee payslips for a payroll run")
    public ResponseEntity<ApiResponse<List<PayrollDetailResponse>>> getDetails(@PathVariable Long runId) {
        log.debug("PayrollController:getDetails :: runId={}", runId);
        return ResponseEntity.ok(ApiResponse.ok(payrollService.getDetails(runId)));
    }

    @GetMapping("/employee/{empId}")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN','ROLE_EMPLOYEE')")
    @Operation(summary = "Get employee payslip", description = "Fetch individual payslip for a specific month")
    public ResponseEntity<ApiResponse<PayrollDetailResponse>> getPayslip(
            @PathVariable Long empId,
            @RequestParam int year,
            @RequestParam int month) {
        log.debug("PayrollController:getPayslip :: empId={} {}/{}", empId, year, month);
        return ResponseEntity.ok(ApiResponse.ok(payrollService.getEmployeePayslip(empId, year, month)));
    }
}
