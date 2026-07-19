package com.business.erp.employee.controller;

import com.business.erp.common.response.ApiResponse;
import com.business.erp.common.response.PageResponse;
import com.business.erp.employee.dto.request.CreateEmployeeRequest;
import com.business.erp.employee.dto.request.UpdateEmployeeRequest;
import com.business.erp.employee.dto.request.UpdateSalaryRequest;
import com.business.erp.employee.dto.response.EmployeeResponse;
import com.business.erp.employee.dto.response.SalaryHistoryResponse;
import com.business.erp.employee.entity.Employee;
import com.business.erp.employee.service.EmployeeService;
import io.swagger.v3.oas.annotations.Operation;
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
@RequestMapping("/api/v1/employees")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Employees", description = "Employee lifecycle management — create, update, salary history, deactivate")
public class EmployeeController {

    private final EmployeeService employeeService;
    private final Logger log = LoggerFactory.getLogger(EmployeeController.class);

    @PostMapping
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @Operation(summary = "Create employee", description = "Register a new employee with initial salary record")
    public ResponseEntity<ApiResponse<EmployeeResponse>> create(
            @Valid @RequestBody CreateEmployeeRequest request,
            @AuthenticationPrincipal UserDetails user) {
        log.info("EmployeeController:create :: by={}", user.getUsername());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(employeeService.create(request, user.getUsername()), "Employee created"));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ROLE_ADMIN','ROLE_MANAGER')")
    @Operation(summary = "List employees", description = "Paginated search with optional status and name/code filter")
    public ResponseEntity<ApiResponse<PageResponse<EmployeeResponse>>> getAll(
            @RequestParam(required = false) Employee.EmployeeStatus status,
            @RequestParam(required = false) String search,
            @PageableDefault(size = 20, sort = "name") Pageable pageable) {
        log.debug("EmployeeController:getAll :: status={} search={}", status, search);
        return ResponseEntity.ok(ApiResponse.ok(
                PageResponse.of(employeeService.findAll(status, search, pageable))));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN','ROLE_MANAGER','ROLE_EMPLOYEE')")
    @Operation(summary = "Get employee by ID")
    public ResponseEntity<ApiResponse<EmployeeResponse>> getById(@PathVariable Long id) {
        log.debug("EmployeeController:getById :: id={}", id);
        return ResponseEntity.ok(ApiResponse.ok(employeeService.findById(id)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @Operation(summary = "Update employee details", description = "Update name, phone, address and designation")
    public ResponseEntity<ApiResponse<EmployeeResponse>> update(
            @PathVariable Long id, @Valid @RequestBody UpdateEmployeeRequest request) {
        log.info("EmployeeController:update :: id={}", id);
        return ResponseEntity.ok(ApiResponse.ok(employeeService.update(id, request)));
    }

    @PutMapping("/{id}/salary")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @Operation(summary = "Update salary", description = "Update employee salary — previous salary is preserved in history")
    public ResponseEntity<ApiResponse<EmployeeResponse>> updateSalary(
            @PathVariable Long id, @Valid @RequestBody UpdateSalaryRequest request,
            @AuthenticationPrincipal UserDetails user) {
        log.info("EmployeeController:updateSalary :: id={} by={}", id, user.getUsername());
        return ResponseEntity.ok(ApiResponse.ok(
                employeeService.updateSalary(id, request, user.getUsername()), "Salary updated"));
    }

    @GetMapping("/{id}/salary-history")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN','ROLE_EMPLOYEE')")
    @Operation(summary = "Get salary history", description = "View full salary change history for an employee")
    public ResponseEntity<ApiResponse<List<SalaryHistoryResponse>>> getSalaryHistory(@PathVariable Long id) {
        log.debug("EmployeeController:getSalaryHistory :: id={}", id);
        return ResponseEntity.ok(ApiResponse.ok(employeeService.getSalaryHistory(id)));
    }

    @PutMapping("/{id}/deactivate")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @Operation(summary = "Deactivate employee", description = "Soft-deactivate an employee — all data is retained")
    public ResponseEntity<ApiResponse<EmployeeResponse>> deactivate(@PathVariable Long id) {
        log.info("EmployeeController:deactivate :: id={}", id);
        return ResponseEntity.ok(ApiResponse.ok(employeeService.deactivate(id), "Employee deactivated"));
    }
}
