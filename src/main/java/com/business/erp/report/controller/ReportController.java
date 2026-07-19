package com.business.erp.report.controller;

import com.business.erp.common.response.ApiResponse;
import com.business.erp.report.dto.AttendanceReportResponse;
import com.business.erp.report.dto.PayrollReportResponse;
import com.business.erp.report.dto.ProfitLossResponse;
import com.business.erp.report.service.ReportService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/reports")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Reports", description = "Reports operations")
public class ReportController {

    private final ReportService reportService;
    private final Logger log = LoggerFactory.getLogger(ReportController.class);

    @GetMapping("/attendance")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN','ROLE_MANAGER')")
    public ResponseEntity<ApiResponse<AttendanceReportResponse>> attendance(
            @RequestParam int year, @RequestParam int month) {
        return ResponseEntity.ok(ApiResponse.ok(reportService.attendanceReport(year, month)));
    }

    @GetMapping("/payroll")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<ApiResponse<PayrollReportResponse>> payroll(
            @RequestParam int year, @RequestParam int month) {
        return ResponseEntity.ok(ApiResponse.ok(reportService.payrollReport(year, month)));
    }

    @GetMapping("/pl")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<ApiResponse<ProfitLossResponse>> profitLoss(
            @RequestParam int year, @RequestParam int month) {
        return ResponseEntity.ok(ApiResponse.ok(reportService.profitLossReport(year, month)));
    }

    @GetMapping("/pl/yearly")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<ApiResponse<ProfitLossResponse>> yearlyPL(@RequestParam int year) {
        return ResponseEntity.ok(ApiResponse.ok(reportService.yearlyPLReport(year)));
    }
}
