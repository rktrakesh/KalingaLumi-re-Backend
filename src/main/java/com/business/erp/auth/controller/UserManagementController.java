package com.business.erp.auth.controller;

import com.business.erp.auth.dto.request.CreateUserRequest;
import com.business.erp.auth.dto.response.UserProfileResponse;
import com.business.erp.auth.entity.User;
import com.business.erp.auth.repository.UserRepository;
import com.business.erp.common.exception.BusinessException;
import com.business.erp.common.exception.ResourceNotFoundException;
import com.business.erp.common.response.ApiResponse;
import com.business.erp.employee.entity.Employee;
import com.business.erp.employee.repository.EmployeeRepository;
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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('ROLE_ADMIN')")
@Tag(name = "User Management", description = "Create and manage system user accounts — assign roles and link to employees (Admin only)")
public class UserManagementController {

    private final UserRepository userRepository;
    private final EmployeeRepository employeeRepository;
    private final PasswordEncoder passwordEncoder;
    private final Logger log = LoggerFactory.getLogger(UserManagementController.class);

    @PostMapping
    @Operation(summary = "Create user", description = "Create a new system user with role assignment and optional employee linkage")
    public ResponseEntity<ApiResponse<UserProfileResponse>> createUser(
            @Valid @RequestBody CreateUserRequest req) {
        log.info("UserManagementController:createUser :: username={} role={}", req.getUsername(), req.getRole());
        if (userRepository.existsByUsername(req.getUsername()))
            throw new BusinessException("Username already exists: " + req.getUsername());

        Employee employee = req.getEmployeeId() != null
                ? employeeRepository.findById(req.getEmployeeId())
                .orElseThrow(() -> new ResourceNotFoundException("Employee", req.getEmployeeId()))
                : null;

        User user = User.builder()
                .username(req.getUsername()).fullName(req.getFullName())
                .passwordHash(passwordEncoder.encode(req.getPassword()))
                .role(req.getRole()).employee(employee)
                .status(User.UserStatus.ACTIVE).build();
        user.setCreatedDate(LocalDateTime.now());
        User saved = userRepository.save(user);
        log.info("UserManagementController:createUser :: SUCCESS id={} username={}", saved.getId(), saved.getUsername());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(toProfile(saved), "User created"));
    }

    @GetMapping
    @Operation(summary = "List all users", description = "Retrieve all system users with their roles and status")
    public ResponseEntity<ApiResponse<List<UserProfileResponse>>> getAll() {
        log.debug("UserManagementController:getAll :: Fetching all users");
        return ResponseEntity.ok(ApiResponse.ok(
                userRepository.findAll().stream().map(this::toProfile).collect(Collectors.toList())));
    }

    @PutMapping("/{id}/deactivate")
    @Operation(summary = "Deactivate user", description = "Deactivate a system user account — they can no longer log in")
    public ResponseEntity<ApiResponse<UserProfileResponse>> deactivate(@PathVariable Long id) {
        log.info("UserManagementController:deactivate :: id={}", id);
        User user = userRepository.findById(id)
                .orElseThrow(() -> new BusinessException("User not found: " + id));
        user.setStatus(User.UserStatus.INACTIVE);
        return ResponseEntity.ok(ApiResponse.ok(toProfile(userRepository.save(user)), "User deactivated"));
    }

    private UserProfileResponse toProfile(User u) {
        return UserProfileResponse.builder()
                .id(u.getId())
                .username(u.getUsername())
                .fullName(u.getFullName())
                .role(u.getRole().name())
                .employeeId(u.getEmployeeId())
                .status(u.getStatus().name())
                .mustChangePassword(u.getMustChangePassword())
                .build();
    }
}