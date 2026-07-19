package com.business.erp.auth.controller;

import com.business.erp.auth.dto.request.ChangePasswordRequest;
import com.business.erp.auth.dto.request.LoginRequest;
import com.business.erp.auth.dto.request.RefreshTokenRequest;
import com.business.erp.auth.dto.response.TokenResponse;
import com.business.erp.auth.dto.response.UserProfileResponse;
import com.business.erp.auth.service.AuthService;
import com.business.erp.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Login, logout, token refresh and password management")
public class AuthController {

    private final AuthService authService;
    private final Logger log =  LoggerFactory.getLogger(AuthController.class);

    @PostMapping("/login")
    @Operation(summary = "Login", description = "Authenticate with username and password to receive JWT tokens")
    public ResponseEntity<ApiResponse<TokenResponse>> login(@Valid @RequestBody LoginRequest request) {
        log.info("AuthController:login :: username={}", request.getUsername());
        return ResponseEntity.ok(ApiResponse.ok(authService.login(request), "Login successful"));
    }

    @PostMapping("/refresh")
    @Operation(summary = "Refresh token", description = "Exchange a valid refresh token for a new access token")
    public ResponseEntity<ApiResponse<TokenResponse>> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        log.debug("AuthController:refresh :: Processing token refresh");
        return ResponseEntity.ok(ApiResponse.ok(authService.refreshToken(request.getRefreshToken())));
    }

    @PostMapping("/logout")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Logout", description = "Revoke the current user's refresh tokens")
    public ResponseEntity<ApiResponse<Void>> logout(@AuthenticationPrincipal UserDetails user) {
        log.info("AuthController:logout :: username={}", user.getUsername());
        authService.logout(user.getUsername());
        return ResponseEntity.ok(ApiResponse.ok("Logged out successfully"));
    }

    @GetMapping("/me")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Get profile", description = "Retrieve the currently authenticated user's profile")
    public ResponseEntity<ApiResponse<UserProfileResponse>> me(@AuthenticationPrincipal UserDetails user) {
        log.debug("AuthController:me :: username={}", user.getUsername());
        return ResponseEntity.ok(ApiResponse.ok(authService.getProfile(user.getUsername())));
    }

    @PutMapping("/change-password")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Change password", description = "Change password for the currently authenticated user")
    public ResponseEntity<ApiResponse<Void>> changePassword(@AuthenticationPrincipal UserDetails user,
                                                            @Valid @RequestBody ChangePasswordRequest request) {
        log.info("AuthController:changePassword :: username={}", user.getUsername());
        authService.changePassword(user.getUsername(), request);
        return ResponseEntity.ok(ApiResponse.ok("Password changed successfully"));
    }
}
