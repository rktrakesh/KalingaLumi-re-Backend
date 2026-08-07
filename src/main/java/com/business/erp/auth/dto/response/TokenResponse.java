package com.business.erp.auth.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.Set;

@Data
@Builder
public class TokenResponse {
    private String accessToken;
    private String refreshToken;
    private String tokenType;
    private String username;
    private String fullName;
    private String role;
    private Set<String> roles;
    private Boolean mustChangePassword;
}
