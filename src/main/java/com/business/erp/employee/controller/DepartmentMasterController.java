package com.business.erp.employee.controller;

import com.business.erp.common.response.ApiResponse;
import com.business.erp.employee.dto.request.CreateMasterDataRequest;
import com.business.erp.employee.dto.request.UpdateMasterDataRequest;
import com.business.erp.employee.dto.response.MasterDataResponse;
import com.business.erp.employee.service.DepartmentMasterService;
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
@RequestMapping("/api/v1/masters/departments")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Department Master", description = "Master data for organizational departments")
public class DepartmentMasterController {

    private final DepartmentMasterService service;
    private final Logger log = LoggerFactory.getLogger(DepartmentMasterController.class);

    @PostMapping
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @Operation(summary = "Create a new department", description = "Creates a new department in the system. Requires admin privileges.")
    public ResponseEntity<ApiResponse<MasterDataResponse>> create(
            @Valid @RequestBody CreateMasterDataRequest request, @AuthenticationPrincipal UserDetails user) {
        log.info("DepartmentMasterController:create :: by={}", user.getUsername());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(service.create(request, user.getUsername()), "Department created"));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @Operation(summary = "Update a department", description = "Updates an existing department in the system. Requires admin privileges.")
    public ResponseEntity<ApiResponse<MasterDataResponse>> update(
            @PathVariable Long id, @Valid @RequestBody UpdateMasterDataRequest request, @AuthenticationPrincipal UserDetails user) {
        log.info("DepartmentMasterController:update :: id={} by={}", id, user.getUsername());
        return ResponseEntity.ok(ApiResponse.ok(service.update(id, request, user.getUsername()), "Department updated"));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN','ROLE_HR','ROLE_MANAGER','ROLE_EMPLOYEE')")
    @Operation(summary = "Get department by ID", description = "Retrieves a department by its ID.")
    public ResponseEntity<ApiResponse<MasterDataResponse>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(service.findById(id)));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ROLE_ADMIN','ROLE_HR','ROLE_MANAGER','ROLE_EMPLOYEE')")
    @Operation(summary = "List departments", description = "Returns a list of all departments.")
    public ResponseEntity<ApiResponse<List<MasterDataResponse>>> findAll(
            @RequestParam(defaultValue = "true") boolean activeOnly) {
        return ResponseEntity.ok(ApiResponse.ok(service.findAll(activeOnly)));
    }
}
