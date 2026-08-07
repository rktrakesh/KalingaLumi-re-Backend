package com.business.erp.attendance.controller;

import com.business.erp.attendance.dto.AttendanceResponse;
import com.business.erp.attendance.dto.CheckInRequest;
import com.business.erp.attendance.dto.CheckOutRequest;
import com.business.erp.attendance.dto.CorrectAttendanceRequest;
import com.business.erp.attendance.entity.AttendanceRecord;
import com.business.erp.attendance.service.AttendanceService;
import com.business.erp.auth.service.AuthenticatedEmployeeAccessService;
import com.business.erp.auth.entity.User;
import com.business.erp.common.clock.ClockProvider;
import com.business.erp.common.exception.BusinessException;
import com.business.erp.common.response.ApiResponse;
import com.business.erp.common.response.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/attendance")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Attendance", description = "Daily check-in/check-out, correction, pending checkout resolution and monthly summary")
public class AttendanceController {

    private final AttendanceService attendanceService;
    private final AuthenticatedEmployeeAccessService employeeAccessService;
    private final ClockProvider clockProvider;
    private final Logger log = LoggerFactory.getLogger(AttendanceController.class);

    @PostMapping("/check-in")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN','ROLE_SUPERVISOR','ROLE_EMPLOYEE')")
    @Operation(summary = "Record check-in", description = "Mark an employee as PRESENT with check-in time")
    public ResponseEntity<ApiResponse<AttendanceResponse>> checkIn(
            @Valid @RequestBody CheckInRequest request,
            @AuthenticationPrincipal UserDetails user) {
        employeeAccessService.requireAdminSupervisorOrSelf(user, request.getEmployeeId());
        if (user instanceof User appUser
                && appUser.getRoles().contains(User.Role.ROLE_EMPLOYEE)
                && !appUser.getRoles().contains(User.Role.ROLE_ADMIN)
                && !appUser.getRoles().contains(User.Role.ROLE_SUPERVISOR)
                && !clockProvider.today().equals(request.getAttendanceDate())) {
            throw new BusinessException("Employees can record attendance only for today");
        }
        log.info("AttendanceController:checkIn :: empId={} date={} by={}", request.getEmployeeId(), request.getAttendanceDate(), user.getUsername());
        return ResponseEntity.ok(ApiResponse.ok(attendanceService.checkIn(request, user.getUsername())));
    }

    @PutMapping("/{id}/check-out")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN','ROLE_SUPERVISOR','ROLE_EMPLOYEE')")
    @Operation(summary = "Record check-out", description = "Record check-out time and calculate worked minutes. Auto-raises overtime request if worked hours exceed standard hours")
    public ResponseEntity<ApiResponse<AttendanceResponse>> checkOut(
            @PathVariable Long id,
            @Valid @RequestBody CheckOutRequest request,
            @AuthenticationPrincipal UserDetails user) {
        AttendanceResponse attendance = attendanceService.getById(id);
        employeeAccessService.requireAdminSupervisorOrSelf(user, attendance.getEmployeeId());
        log.info("AttendanceController:checkOut :: attendanceId={}", id);
        return ResponseEntity.ok(ApiResponse.ok(attendanceService.checkOut(id, request)));
    }

    @PutMapping("/{id}/correct")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @Operation(summary = "Correct attendance", description = "Admin-only correction of attendance within 7-day window. Blocked if month is closed. Requires remarks.")
    public ResponseEntity<ApiResponse<AttendanceResponse>> correct(
            @PathVariable Long id,
            @Valid @RequestBody CorrectAttendanceRequest request,
            @AuthenticationPrincipal UserDetails user) {
        log.info("AttendanceController:correct :: id={} by={}", id, user.getUsername());
        return ResponseEntity.ok(ApiResponse.ok(attendanceService.correct(id, request, user.getUsername()), "Attendance corrected"));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ROLE_ADMIN','ROLE_MANAGER','ROLE_SUPERVISOR')")
    @Operation(summary = "Search attendance", description = "Search attendance records with filters: employeeId, date, status, date range")
    public ResponseEntity<ApiResponse<PageResponse<AttendanceResponse>>> search(
            @RequestParam(required = false) Long employeeId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) AttendanceRecord.AttendanceStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @PageableDefault(size = 50) Pageable pageable) {
        log.debug("AttendanceController:search :: empId={} status={}", employeeId, status);
        return ResponseEntity.ok(ApiResponse.ok(attendanceService.search(employeeId, date, status, from, to, pageable)));
    }

    @GetMapping("/{employeeId}/monthly")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN','ROLE_MANAGER','ROLE_EMPLOYEE')")
    @Operation(summary = "Monthly attendance", description = "Get full monthly attendance list for an employee")
    public ResponseEntity<ApiResponse<List<AttendanceResponse>>> getMonthly(
            @PathVariable Long employeeId,
            @RequestParam int year,
            @RequestParam int month,
            @AuthenticationPrincipal UserDetails user) {
        Long authorizedEmployeeId = employeeAccessService
                .requireAdminManagerOrSelf(user, employeeId);
        log.debug("AttendanceController:getMonthly :: empId={} {}/{}", employeeId, year, month);
        return ResponseEntity.ok(ApiResponse.ok(
                attendanceService.getMonthlyAttendance(authorizedEmployeeId, year, month)));
    }

    @GetMapping("/pending-checkout")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @Operation(summary = "Pending checkouts", description = "List all employees with PENDING_CHECKOUT status requiring Admin resolution")
    public ResponseEntity<ApiResponse<List<AttendanceResponse>>> getPendingCheckouts() {
        log.debug("AttendanceController:getPendingCheckouts :: Fetching all pending checkouts");
        return ResponseEntity.ok(ApiResponse.ok(attendanceService.getPendingCheckouts()));
    }
}
