package com.business.erp.auth.service;

import com.business.erp.auth.dto.request.ChangePasswordRequest;
import com.business.erp.auth.dto.request.LoginRequest;
import com.business.erp.auth.dto.response.TokenResponse;
import com.business.erp.auth.dto.response.UserProfileResponse;

public interface AuthService {
    TokenResponse login(LoginRequest request);

    TokenResponse refreshToken(String token);

    void logout(String username);

    TokenResponse changePassword(String username, ChangePasswordRequest request);

    UserProfileResponse getProfile(String username);

    void forgotPassword(String email);

    void resetPassword(String token, String newPassword);
}
