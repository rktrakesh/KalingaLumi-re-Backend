package com.business.erp.auth.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Set;

@Data
@Builder
public class UserProfileResponse {
    private Long id;
    private String username;
    private String fullName;
    private String role;
    private Set<String> roles;
    private Long employeeId;
    private String status;
    private Boolean mustChangePassword;
    private Boolean credentialsExpired;
    private Integer failedLoginAttempts;
    private LocalDateTime lockedAt;
    private LocalDateTime lastLoginAt;
    private LocalDateTime temporaryPasswordIssuedAt;
    private LocalDateTime temporaryPasswordExpiresAt;
    private Boolean enabled;
    private Boolean accountNonLocked;
    private String employeeCategory;
}
