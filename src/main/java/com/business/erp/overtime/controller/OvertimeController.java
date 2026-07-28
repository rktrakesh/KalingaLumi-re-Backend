package com.business.erp.overtime.controller;

import com.business.erp.common.response.ApiResponse;
import com.business.erp.common.response.PageResponse;
import com.business.erp.overtime.dto.request.ApproveOvertimeRequest;
import com.business.erp.overtime.dto.request.ConvertLeaveToOTRequest;
import com.business.erp.overtime.dto.request.ReopenOvertimeRequest;
import com.business.erp.overtime.dto.response.OvertimeResponse;
import com.business.erp.overtime.entity.OvertimeRequest;
import com.business.erp.overtime.service.OvertimeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/overtime")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Overtime", description = "Approve/reject overtime requests and convert unused leaves to overtime (Admin only)")
public class OvertimeController {

    private final OvertimeService overtimeService;
    private final Logger log = LoggerFactory.getLogger(OvertimeController.class);

    @GetMapping
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @Operation(summary = "Search overtime requests", description = "Filter by employeeId and/or status")
    public ResponseEntity<ApiResponse<PageResponse<OvertimeResponse>>> search(
            @RequestParam(required = false) Long employeeId,
            @RequestParam(required = false) OvertimeRequest.OvertimeStatus status,
            @PageableDefault(size = 20) Pageable pageable) {
        log.debug("OvertimeController:search :: empId={} status={}", employeeId, status);
        return ResponseEntity.ok(ApiResponse.ok(overtimeService.search(employeeId, status, pageable)));
    }

    @PutMapping("/{id}/approve")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @Operation(summary = "Approve overtime", description = "Approve or partially approve an overtime request")
    public ResponseEntity<ApiResponse<OvertimeResponse>> approve(
            @PathVariable Long id, @Valid @RequestBody ApproveOvertimeRequest req,
            @AuthenticationPrincipal UserDetails user) {
        log.info("OvertimeController:approve :: id={} by={}", id, user.getUsername());
        return ResponseEntity.ok(ApiResponse.ok(overtimeService.approve(id, req, user.getUsername()), "Overtime approved"));
    }

    @PutMapping("/{id}/reject")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @Operation(summary = "Reject overtime", description = "Reject a pending overtime request with optional remarks")
    public ResponseEntity<ApiResponse<OvertimeResponse>> reject(
            @PathVariable Long id, @RequestParam(required = false) String remarks,
            @AuthenticationPrincipal UserDetails user) {
        log.info("OvertimeController:reject :: id={} by={}", id, user.getUsername());
        return ResponseEntity.ok(ApiResponse.ok(overtimeService.reject(id, remarks, user.getUsername()), "Overtime rejected"));
    }

    @PutMapping("/{id}/reopen")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @Operation(summary = "Reopen approved overtime", description = "Reopens an APPROVED/MODIFIED overtime request so the attendance underneath it can be corrected. Requires a reason.")
    public ResponseEntity<ApiResponse<OvertimeResponse>> reopen(
            @PathVariable Long id, @Valid @RequestBody ReopenOvertimeRequest req,
            @AuthenticationPrincipal UserDetails user) {
        log.info("OvertimeController:reopen :: id={} by={} reason={}", id, user.getUsername(), req.getReason());
        return ResponseEntity.ok(ApiResponse.ok(overtimeService.reopen(id, req, user.getUsername()),
                "Overtime request reopened — attendance can now be corrected"));
    }

    @PostMapping("/convert-leave")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @Operation(summary = "Convert unused leaves to overtime", description = "Create LEAVE_CONVERSION overtime requests for unused paid leaves at month end")
    public ResponseEntity<ApiResponse<List<OvertimeResponse>>> convertLeave(
            @Valid @RequestBody ConvertLeaveToOTRequest req,
            @AuthenticationPrincipal UserDetails user) {
        log.info("OvertimeController:convertLeave :: empId={} days={} by={}", req.getEmployeeId(), req.getUnusedLeaveDays(), user.getUsername());
        return ResponseEntity.ok(ApiResponse.ok(
                overtimeService.convertUnusedLeaves(req, user.getUsername()), "Leave conversion OT requests created"));
    }
}