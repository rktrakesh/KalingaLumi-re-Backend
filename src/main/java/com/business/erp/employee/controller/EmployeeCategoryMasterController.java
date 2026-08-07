package com.business.erp.employee.controller;

import com.business.erp.common.response.ApiResponse;
import com.business.erp.employee.dto.request.CreateMasterDataRequest;
import com.business.erp.employee.dto.request.UpdateMasterDataRequest;
import com.business.erp.employee.dto.response.MasterDataResponse;
import com.business.erp.employee.service.EmployeeCategoryMasterService;
import io.swagger.v3.oas.annotations.Operation;
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

import java.util.List;

@RestController
@RequestMapping("/api/v1/masters/employee-categories")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Employee Category Master", description = "Master data for FACTORY/SALES/ADMINISTRATION/MANAGEMENT and any future category")
public class EmployeeCategoryMasterController {

    private final EmployeeCategoryMasterService service;
    private final Logger log = LoggerFactory.getLogger(EmployeeCategoryMasterController.class);

    @PostMapping
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @Operation(summary = "Create employee category")
    public ResponseEntity<ApiResponse<MasterDataResponse>> create(
            @Valid @RequestBody CreateMasterDataRequest request, @AuthenticationPrincipal UserDetails user) {
        log.info("EmployeeCategoryMasterController:create :: by={}", user.getUsername());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(service.create(request, user.getUsername()), "Employee category created"));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @Operation(summary = "Update employee category", description = "Code is immutable and cannot be changed here")
    public ResponseEntity<ApiResponse<MasterDataResponse>> update(
            @PathVariable Long id, @Valid @RequestBody UpdateMasterDataRequest request, @AuthenticationPrincipal UserDetails user) {
        log.info("EmployeeCategoryMasterController:update :: id={} by={}", id, user.getUsername());
        return ResponseEntity.ok(ApiResponse.ok(service.update(id, request, user.getUsername()), "Employee category updated"));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN','ROLE_HR','ROLE_MANAGER','ROLE_EMPLOYEE')")
    public ResponseEntity<ApiResponse<MasterDataResponse>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(service.findById(id)));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ROLE_ADMIN','ROLE_HR','ROLE_MANAGER','ROLE_EMPLOYEE')")
    @Operation(summary = "List employee categories", description = "activeOnly=true (default) returns only active categories — used to populate dropdowns")
    public ResponseEntity<ApiResponse<List<MasterDataResponse>>> findAll(
            @RequestParam(defaultValue = "true") boolean activeOnly) {
        return ResponseEntity.ok(ApiResponse.ok(service.findAll(activeOnly)));
    }
}
