package com.business.erp.auth.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserProfileResponse {
    private Long id;
    private String username;
    private String fullName;
    private String role;
    private Long employeeId;
    private String status;
    private Boolean mustChangePassword;
    private String employeeCategory;
}