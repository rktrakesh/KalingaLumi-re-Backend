package com.business.erp.auth.controller;

import com.business.erp.auth.dto.request.CreateUserRequest;
import com.business.erp.auth.dto.response.UserProfileResponse;
import com.business.erp.auth.dto.response.TemporaryPasswordResetResponse;
import com.business.erp.auth.entity.User;
import com.business.erp.auth.service.UserManagementService;
import com.business.erp.common.response.ApiResponse;
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
import java.util.LinkedHashSet;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('ROLE_ADMIN')")
@Tag(name = "User Management", description = "Create and manage system user accounts — assign roles and link to employees (Admin only)")
public class UserManagementController {

    private final UserManagementService userManagementService;
    private final Logger log = LoggerFactory.getLogger(UserManagementController.class);

    @PostMapping
    @Operation(summary = "Create user", description = "Create a new system user with role assignment and optional employee linkage")
    public ResponseEntity<ApiResponse<UserProfileResponse>> createUser(
            @Valid @RequestBody CreateUserRequest request,
            @AuthenticationPrincipal UserDetails actor) {
        User saved = userManagementService.create(request, actor.getUsername());
        log.info("UserManagementController:createUser :: created userId={}", saved.getId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(toProfile(saved), "User created"));
    }

    @GetMapping
    @Operation(summary = "List all users", description = "Retrieve all system users with their roles and status")
    public ResponseEntity<ApiResponse<List<UserProfileResponse>>> getAll() {
        log.debug("UserManagementController:getAll :: Fetching all users");
        return ResponseEntity.ok(ApiResponse.ok(
                userManagementService.findAll().stream()
                        .map(this::toProfile)
                        .collect(Collectors.toList())));
    }

    @PutMapping("/{id}/deactivate")
    @Operation(summary = "Deactivate user", description = "Deactivate a system user account — they can no longer log in")
    public ResponseEntity<ApiResponse<UserProfileResponse>> deactivate(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails actor) {
        return ResponseEntity.ok(ApiResponse.ok(
                toProfile(userManagementService.disable(id, actor.getUsername())), "User deactivated"));
    }

    @PutMapping("/{id}/enable")
    @Operation(summary = "Enable user")
    public ResponseEntity<ApiResponse<UserProfileResponse>> enable(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails actor) {
        return ResponseEntity.ok(ApiResponse.ok(
                toProfile(userManagementService.enable(id, actor.getUsername())), "User enabled"));
    }

    @PutMapping("/{id}/lock")
    @Operation(summary = "Lock user")
    public ResponseEntity<ApiResponse<UserProfileResponse>> lock(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails actor) {
        return ResponseEntity.ok(ApiResponse.ok(
                toProfile(userManagementService.lock(id, actor.getUsername())), "User locked"));
    }

    @PutMapping("/{id}/unlock")
    @Operation(summary = "Unlock user")
    public ResponseEntity<ApiResponse<UserProfileResponse>> unlock(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails actor) {
        return ResponseEntity.ok(ApiResponse.ok(
                toProfile(userManagementService.unlock(id, actor.getUsername())), "User unlocked"));
    }

    @PostMapping("/{id}/reset-temporary-password")
    @Operation(summary = "Reset temporary password",
            description = "Generate a one-time temporary password and revoke existing tokens")
    public ResponseEntity<ApiResponse<TemporaryPasswordResetResponse>> resetTemporaryPassword(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails actor) {
        return ResponseEntity.ok(ApiResponse.ok(
                userManagementService.resetTemporaryPassword(id, actor.getUsername()),
                "Temporary password generated; record it now"));
    }

    @PutMapping("/{id}/roles/{role}")
    @Operation(summary = "Assign role")
    public ResponseEntity<ApiResponse<UserProfileResponse>> assignRole(
            @PathVariable Long id,
            @PathVariable User.Role role,
            @AuthenticationPrincipal UserDetails actor) {
        return ResponseEntity.ok(ApiResponse.ok(
                toProfile(userManagementService.assignRole(id, role, actor.getUsername())),
                "Role assigned"));
    }

    @DeleteMapping("/{id}/roles/{role}")
    @Operation(summary = "Remove role")
    public ResponseEntity<ApiResponse<UserProfileResponse>> removeRole(
            @PathVariable Long id,
            @PathVariable User.Role role,
            @AuthenticationPrincipal UserDetails actor) {
        return ResponseEntity.ok(ApiResponse.ok(
                toProfile(userManagementService.removeRole(id, role, actor.getUsername())),
                "Role removed"));
    }

    @PutMapping("/{id}/employee/{employeeId}")
    @Operation(summary = "Link employee")
    public ResponseEntity<ApiResponse<UserProfileResponse>> linkEmployee(
            @PathVariable Long id,
            @PathVariable Long employeeId,
            @AuthenticationPrincipal UserDetails actor) {
        return ResponseEntity.ok(ApiResponse.ok(toProfile(
                userManagementService.linkEmployee(id, employeeId, actor.getUsername())), "Employee linked"));
    }

    @DeleteMapping("/{id}/employee")
    @Operation(summary = "Unlink employee")
    public ResponseEntity<ApiResponse<UserProfileResponse>> unlinkEmployee(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails actor) {
        return ResponseEntity.ok(ApiResponse.ok(toProfile(
                userManagementService.unlinkEmployee(id, actor.getUsername())), "Employee unlinked"));
    }

    private UserProfileResponse toProfile(User u) {
        return UserProfileResponse.builder()
                .id(u.getId())
                .username(u.getUsername())
                .fullName(u.getFullName())
                .role(u.getRole().name())
                .roles(u.getRoles().stream()
                        .sorted()
                        .map(Enum::name)
                        .collect(Collectors.toCollection(LinkedHashSet::new)))
                .employeeId(u.getEmployeeId())
                .status(u.getStatus().name())
                .mustChangePassword(u.getMustChangePassword())
                .credentialsExpired(u.getCredentialsExpired())
                .failedLoginAttempts(u.getFailedLoginAttempts())
                .lockedAt(u.getLockedAt())
                .lastLoginAt(u.getLastLoginAt())
                .temporaryPasswordIssuedAt(u.getTemporaryPasswordIssuedAt())
                .temporaryPasswordExpiresAt(u.getTemporaryPasswordExpiresAt())
                .enabled(u.isEnabled())
                .accountNonLocked(u.isAccountNonLocked())
                .build();
    }
}
