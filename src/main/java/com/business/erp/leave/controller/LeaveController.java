package com.business.erp.leave.controller;

import com.business.erp.auth.controller.UserManagementController;
import com.business.erp.common.response.ApiResponse;
import com.business.erp.common.response.PageResponse;
import com.business.erp.leave.dto.request.ApproveRejectLeaveRequest;
import com.business.erp.leave.dto.response.LeaveBalanceResponse;
import com.business.erp.leave.dto.request.LeaveRequestDto;
import com.business.erp.leave.dto.response.LeaveResponse;
import com.business.erp.leave.entity.LeaveRequest;
import com.business.erp.leave.service.LeaveService;
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
@RequestMapping("/api/v1/leaves")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Leave", description = "Leave management")
public class LeaveController {

    private final LeaveService leaveService;
    private final Logger log = LoggerFactory.getLogger(LeaveController.class);

    @PostMapping("/request")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN','ROLE_EMPLOYEE')")
    public ResponseEntity<ApiResponse<LeaveResponse>> createRequest(
            @Valid @RequestBody LeaveRequestDto req,
            @AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(leaveService.createRequest(req, user.getUsername()), "Leave request submitted"));
    }

    @GetMapping
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<ApiResponse<PageResponse<LeaveResponse>>> getAll(
            @RequestParam(required = false) Long employeeId,
            @RequestParam(required = false) LeaveRequest.LeaveStatus status,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.ok(
                PageResponse.of(leaveService.search(employeeId, status, pageable))));
    }

    @GetMapping("/my/{employeeId}")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN','ROLE_EMPLOYEE')")
    public ResponseEntity<ApiResponse<List<LeaveResponse>>> getMyLeaves(@PathVariable Long employeeId) {
        return ResponseEntity.ok(ApiResponse.ok(leaveService.getMyLeaves(employeeId)));
    }

    @PutMapping("/{id}/approve")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<ApiResponse<LeaveResponse>> approve(@PathVariable Long id,
                                                              @AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(ApiResponse.ok(leaveService.approve(id, user.getUsername()), "Leave approved"));
    }

    @PutMapping("/{id}/reject")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<ApiResponse<LeaveResponse>> reject(@PathVariable Long id,
                                                             @RequestBody ApproveRejectLeaveRequest req,
                                                             @AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(ApiResponse.ok(
                leaveService.reject(id, req.getRejectionReason(), user.getUsername()), "Leave rejected"));
    }

    @GetMapping("/balance/{employeeId}")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN','ROLE_EMPLOYEE')")
    public ResponseEntity<ApiResponse<LeaveBalanceResponse>> getBalance(
            @PathVariable Long employeeId,
            @RequestParam int year, @RequestParam int month) {
        return ResponseEntity.ok(ApiResponse.ok(leaveService.getBalance(employeeId, year, month)));
    }
}
