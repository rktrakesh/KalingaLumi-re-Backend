package com.business.erp.performance.controller;

import com.business.erp.common.response.ApiResponse;
import com.business.erp.performance.dto.request.*;
import com.business.erp.performance.dto.response.*;
import com.business.erp.performance.mapper.PerformanceMapper;
import com.business.erp.performance.service.*;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/performance")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Performance Engine", description = "Sales & Marketing performance: targets, incentives, customer ownership, visits, snapshots")
public class PerformanceController {

    private final EmployeeSalesPolicyService salesPolicyService;
    private final CustomerOwnershipService customerOwnershipService;
    private final CustomerVisitService customerVisitService;
    private final PerformanceSnapshotService snapshotService;
    private final PerformanceDashboardService dashboardService;
    private final PerformanceMapper mapper;
    private final Logger log = LoggerFactory.getLogger(PerformanceController.class);

    // ---------------------------------------------------------------- Sales Policy

    @PostMapping("/sales-policies")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN','ROLE_MANAGER')")
    public ResponseEntity<ApiResponse<SalesPolicyResponse>> createSalesPolicy(
            @Valid @RequestBody CreateSalesPolicyRequest req, @AuthenticationPrincipal UserDetails user) {
        log.debug("PerformanceController:createSalesPolicy :: invoked");
        var policy = salesPolicyService.createPolicy(req, user.getUsername());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(mapper.toResponse(policy)));
    }

    @GetMapping("/sales-policies/employee/{employeeId}")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN','ROLE_MANAGER')")
    public ResponseEntity<ApiResponse<SalesPolicyResponse>> getActiveSalesPolicy(@PathVariable Long employeeId) {
        return ResponseEntity.ok(ApiResponse.ok(mapper.toResponse(salesPolicyService.getActivePolicy(employeeId))));
    }

    @GetMapping("/sales-policies/employee/{employeeId}/history")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN','ROLE_MANAGER')")
    public ResponseEntity<ApiResponse<java.util.List<SalesPolicyResponse>>> getSalesPolicyHistory(@PathVariable Long employeeId) {
        return ResponseEntity.ok(ApiResponse.ok(mapper.toResponseList(salesPolicyService.getPolicyHistory(employeeId))));
    }

    // ---------------------------------------------------------------- Customer Ownership

    @PostMapping("/customer-ownership/assign")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN','ROLE_MANAGER')")
    public ResponseEntity<ApiResponse<CustomerOwnershipResponse>> assignOwner(
            @Valid @RequestBody AssignOwnerRequest req, @AuthenticationPrincipal UserDetails user) {
        var ownership = customerOwnershipService.assignOwner(
                req.getCustomerId(), req.getEmployeeId(), req.getEffectiveFrom(), req.getRemarks(), user.getUsername());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(mapper.toResponse(ownership)));
    }

    @PostMapping("/customer-ownership/transfer")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN','ROLE_MANAGER')")
    public ResponseEntity<ApiResponse<java.util.List<CustomerOwnershipResponse>>> transferCustomers(
            @Valid @RequestBody TransferCustomersRequest req, @AuthenticationPrincipal UserDetails user) {
        var transferred = customerOwnershipService.transferCustomers(
                req.getCustomerIds(), req.getToEmployeeId(), req.getEffectiveFrom(), req.getRemarks(), user.getUsername());
        return ResponseEntity.ok(ApiResponse.ok(mapper.toOwnershipResponseList(transferred), "Transferred " + transferred.size() + " customer(s)"));
    }

    @PostMapping("/customer-ownership/assign-temporary")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN','ROLE_MANAGER')")
    public ResponseEntity<ApiResponse<CustomerOwnershipResponse>> assignTemporaryOwner(
            @Valid @RequestBody AssignTemporaryOwnerRequest req, @AuthenticationPrincipal UserDetails user) {
        var ownership = customerOwnershipService.assignTemporary(req.getCustomerId(), req.getEmployeeId(),
                req.getEffectiveFrom(), req.getEffectiveTo(), req.getRemarks(), user.getUsername());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(mapper.toResponse(ownership)));
    }

    @PostMapping("/customer-ownership/{ownershipId}/make-permanent")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN','ROLE_MANAGER')")
    public ResponseEntity<ApiResponse<CustomerOwnershipResponse>> makeTemporaryPermanent(
            @PathVariable Long ownershipId, @AuthenticationPrincipal UserDetails user) {
        var ownership = customerOwnershipService.makeTemporaryPermanent(ownershipId, user.getUsername());
        return ResponseEntity.ok(ApiResponse.ok(mapper.toResponse(ownership), "Temporary assignment made permanent"));
    }

    @GetMapping("/customer-ownership/customer/{customerId}/history")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN','ROLE_MANAGER')")
    public ResponseEntity<ApiResponse<java.util.List<CustomerOwnershipResponse>>> getOwnershipHistory(@PathVariable Long customerId) {
        return ResponseEntity.ok(ApiResponse.ok(mapper.toOwnershipResponseList(customerOwnershipService.getOwnershipHistory(customerId))));
    }

    @GetMapping("/customer-ownership/employee/{employeeId}/assigned-customers")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN','ROLE_MANAGER')")
    public ResponseEntity<ApiResponse<java.util.List<CustomerOwnershipResponse>>> getAssignedCustomers(@PathVariable Long employeeId) {
        return ResponseEntity.ok(ApiResponse.ok(mapper.toOwnershipResponseList(customerOwnershipService.getAssignedCustomers(employeeId))));
    }

    // ---------------------------------------------------------------- Customer Visits

    @PostMapping("/visits")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN','ROLE_MANAGER','ROLE_SUPERVISOR')")
    public ResponseEntity<ApiResponse<CustomerVisitResponse>> logVisit(
            @Valid @RequestBody LogCustomerVisitRequest req, @AuthenticationPrincipal UserDetails user) {
        var visit = customerVisitService.logVisit(req, user.getUsername());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(mapper.toResponse(visit)));
    }

    @GetMapping("/visits/customer/{customerId}")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN','ROLE_MANAGER','ROLE_SUPERVISOR')")
    public ResponseEntity<ApiResponse<java.util.List<CustomerVisitResponse>>> getVisitHistory(@PathVariable Long customerId) {
        return ResponseEntity.ok(ApiResponse.ok(mapper.toVisitResponseList(customerVisitService.getVisitHistory(customerId))));
    }

    // ---------------------------------------------------------------- Performance Snapshot

    @PostMapping("/snapshots/generate/{employeeId}/{year}/{month}")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN','ROLE_MANAGER')")
    public ResponseEntity<ApiResponse<PerformanceSnapshotResponse>> generateSnapshot(
            @PathVariable Long employeeId, @PathVariable int year, @PathVariable int month,
            @AuthenticationPrincipal UserDetails user) {
        var snapshot = snapshotService.generate(employeeId, year, month, user.getUsername());
        return ResponseEntity.ok(ApiResponse.ok(mapper.toResponse(snapshot)));
    }

    @PostMapping("/snapshots/generate-all/{year}/{month}")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN','ROLE_MANAGER')")
    public ResponseEntity<ApiResponse<java.util.List<PerformanceSnapshotResponse>>> generateAllSnapshots(
            @PathVariable int year, @PathVariable int month, @AuthenticationPrincipal UserDetails user) {
        var snapshots = snapshotService.generateForAllSalesEmployees(year, month, user.getUsername());
        return ResponseEntity.ok(ApiResponse.ok(mapper.toSnapshotResponseList(snapshots),
                "Generated " + snapshots.size() + " snapshot(s)"));
    }

    @PostMapping("/snapshots/{snapshotId}/approve")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<ApiResponse<PerformanceSnapshotResponse>> approveSnapshot(
            @PathVariable Long snapshotId, @AuthenticationPrincipal UserDetails user) {
        var snapshot = snapshotService.approve(snapshotId, user.getUsername());
        return ResponseEntity.ok(ApiResponse.ok(mapper.toResponse(snapshot), "Snapshot approved — now read-only"));
    }

    @GetMapping("/snapshots/employee/{employeeId}/{year}/{month}")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN','ROLE_MANAGER')")
    public ResponseEntity<ApiResponse<PerformanceSnapshotResponse>> getSnapshot(
            @PathVariable Long employeeId, @PathVariable int year, @PathVariable int month) {
        return snapshotService.findByEmployeeAndPeriod(employeeId, year, month)
                .map(s -> ResponseEntity.ok(ApiResponse.ok(mapper.toResponse(s))))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/snapshots/employee/{employeeId}/history")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN','ROLE_MANAGER')")
    public ResponseEntity<ApiResponse<java.util.List<PerformanceSnapshotResponse>>> getSnapshotHistory(@PathVariable Long employeeId) {
        return ResponseEntity.ok(ApiResponse.ok(mapper.toSnapshotResponseList(snapshotService.findHistoryForEmployee(employeeId))));
    }

    // ---------------------------------------------------------------- Dashboards

    @GetMapping("/dashboard/employee/{employeeId}/{year}/{month}")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN','ROLE_MANAGER')")
    public ResponseEntity<ApiResponse<EmployeePerformanceDashboardResponse>> getEmployeeDashboard(
            @PathVariable Long employeeId, @PathVariable int year, @PathVariable int month) {
        return ResponseEntity.ok(ApiResponse.ok(dashboardService.getEmployeeDashboard(employeeId, year, month)));
    }

    @GetMapping("/dashboard/management/{year}/{month}")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN','ROLE_MANAGER')")
    public ResponseEntity<ApiResponse<ManagementPerformanceDashboardResponse>> getManagementDashboard(
            @PathVariable int year, @PathVariable int month) {
        return ResponseEntity.ok(ApiResponse.ok(dashboardService.getManagementDashboard(year, month)));
    }
}