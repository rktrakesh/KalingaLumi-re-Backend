package com.business.erp.employee.controller;

import com.business.erp.common.response.ApiResponse;
import com.business.erp.employee.dto.request.CreateDesignationRequest;
import com.business.erp.employee.dto.request.UpdateDesignationRequest;
import com.business.erp.employee.dto.response.DesignationResponse;
import com.business.erp.employee.service.DesignationMasterService;
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
@RequestMapping("/api/v1/masters/designations")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Designation Master", description = "Master data for job designations — each belongs to exactly one Employee Category")
public class DesignationMasterController {

    private final DesignationMasterService service;
    private final Logger log = LoggerFactory.getLogger(DesignationMasterController.class);

    @PostMapping
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<ApiResponse<DesignationResponse>> create(
            @Valid @RequestBody CreateDesignationRequest request, @AuthenticationPrincipal UserDetails user) {
        log.info("DesignationMasterController:create :: by={}", user.getUsername());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(service.create(request, user.getUsername()), "Designation created"));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<ApiResponse<DesignationResponse>> update(
            @PathVariable Long id, @Valid @RequestBody UpdateDesignationRequest request, @AuthenticationPrincipal UserDetails user) {
        log.info("DesignationMasterController:update :: id={} by={}", id, user.getUsername());
        return ResponseEntity.ok(ApiResponse.ok(service.update(id, request, user.getUsername()), "Designation updated"));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN','ROLE_HR','ROLE_MANAGER','ROLE_EMPLOYEE')")
    public ResponseEntity<ApiResponse<DesignationResponse>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(service.findById(id)));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ROLE_ADMIN','ROLE_HR','ROLE_MANAGER','ROLE_EMPLOYEE')")
    @Operation(summary = "List designations", description = "Optionally filter by categoryId — used to populate the Employee form's cascading dropdown")
    public ResponseEntity<ApiResponse<List<DesignationResponse>>> findAll(
            @RequestParam(required = false) Long categoryId,
            @RequestParam(defaultValue = "true") boolean activeOnly) {
        List<DesignationResponse> result = categoryId != null
                ? service.findByCategory(categoryId)
                : service.findAll(activeOnly);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }
}
