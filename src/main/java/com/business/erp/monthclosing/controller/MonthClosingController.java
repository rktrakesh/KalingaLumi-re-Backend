package com.business.erp.monthclosing.controller;

import com.business.erp.common.response.ApiResponse;
import com.business.erp.monthclosing.dto.request.CloseMonthRequest;
import com.business.erp.monthclosing.dto.response.MonthClosingResponse;
import com.business.erp.monthclosing.dto.response.PreCloseCheckResponse;
import com.business.erp.monthclosing.dto.request.ReopenMonthRequest;
import com.business.erp.monthclosing.service.MonthClosingService;
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
@RequestMapping("/api/v1/month-closing")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('ROLE_ADMIN')")
@Tag(name = "Month Closing", description = "Control monthly period locking — close, pre-check validations, reopen (Admin only)")
public class MonthClosingController {

    private final MonthClosingService monthClosingService;
    private final Logger log = LoggerFactory.getLogger(MonthClosingController.class);

    @GetMapping("/status")
    @Operation(summary = "Get month status", description = "Check if a specific month is OPEN or CLOSED")
    public ResponseEntity<ApiResponse<MonthClosingResponse>> getStatus(
            @RequestParam int year, @RequestParam int month) {
        log.debug("MonthClosingController:getStatus :: {}/{}", year, month);
        return ResponseEntity.ok(ApiResponse.ok(monthClosingService.getStatus(year, month)));
    }

    @GetMapping("/pre-check")
    @Operation(summary = "Pre-close check", description = "Validate all conditions before closing the month — returns blockers if any")
    public ResponseEntity<ApiResponse<PreCloseCheckResponse>> preCheck(
            @RequestParam int year, @RequestParam int month) {
        log.info("MonthClosingController:preCheck :: {}/{}", year, month);
        return ResponseEntity.ok(ApiResponse.ok(monthClosingService.preCheck(year, month)));
    }

    @PostMapping("/close")
    @Operation(summary = "Close month", description = "Lock attendance, payroll, expenses and inventory for the specified month")
    public ResponseEntity<ApiResponse<MonthClosingResponse>> close(
            @Valid @RequestBody CloseMonthRequest req,
            @AuthenticationPrincipal UserDetails user) {
        log.info("MonthClosingController:close :: {}/{} by={}", req.getYear(), req.getMonth(), user.getUsername());
        return ResponseEntity.ok(ApiResponse.ok(
                monthClosingService.closeMonth(req.getYear(), req.getMonth(), user.getUsername()),
                "Month closed successfully"));
    }

    @PostMapping("/reopen")
    @Operation(summary = "Reopen month", description = "Unlock a previously closed month — requires remarks")
    public ResponseEntity<ApiResponse<MonthClosingResponse>> reopen(
            @Valid @RequestBody ReopenMonthRequest req,
            @AuthenticationPrincipal UserDetails user) {
        log.info("MonthClosingController:reopen :: {}/{} by={}", req.getYear(), req.getMonth(), user.getUsername());
        return ResponseEntity.ok(ApiResponse.ok(
                monthClosingService.reopenMonth(req.getYear(), req.getMonth(), req.getRemarks(), user.getUsername()),
                "Month reopened"));
    }

    @GetMapping("/history")
    @Operation(summary = "Closing history", description = "View all past month-close and reopen events")
    public ResponseEntity<ApiResponse<List<MonthClosingResponse>>> getHistory() {
        return ResponseEntity.ok(ApiResponse.ok(monthClosingService.getHistory()));
    }
}
