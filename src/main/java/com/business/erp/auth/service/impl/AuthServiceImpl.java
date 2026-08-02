package com.business.erp.auth.service.impl;

import com.business.erp.auth.dto.request.ChangePasswordRequest;
import com.business.erp.auth.dto.request.LoginRequest;
import com.business.erp.auth.dto.response.TokenResponse;
import com.business.erp.auth.dto.response.UserProfileResponse;
import com.business.erp.auth.entity.PasswordResetToken;
import com.business.erp.auth.entity.RefreshToken;
import com.business.erp.auth.entity.User;
import com.business.erp.auth.enums.LoginAuditEventType;
import com.business.erp.auth.repository.PasswordResetTokenRepository;
import com.business.erp.auth.repository.UserRepository;
import com.business.erp.auth.service.*;
import com.business.erp.common.exception.BusinessException;
import com.business.erp.common.exception.ResourceNotFoundException;
import com.business.erp.employee.entity.Employee;
import com.business.erp.employee.repository.EmployeeRepository;
import com.business.erp.settings.enums.SettingKey;
import com.business.erp.settings.service.SettingsService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final EmployeeRepository employeeRepository;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final AuthenticationManager authenticationManager;
    private final PasswordEncoder passwordEncoder;
    private final SettingsService settingsService;
    private final PasswordPolicyService passwordPolicyService;
    private final LoginAuditService loginAuditService;
    private final EmailService emailService;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final Logger log = LoggerFactory.getLogger(AuthServiceImpl.class);

    @Value("${app.frontend.reset-password-url:http://localhost:9999/reset-password}")
    private String resetPasswordUrlBase;

    @Override
    @Transactional
    public TokenResponse login(LoginRequest request) {
        log.info("AuthServiceImpl:login :: Attempting login for username={}", request.getUsername());
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword()));
        } catch (BadCredentialsException ex) {
            recordFailedAttempt(request.getUsername());
            throw ex;
        } catch (LockedException ex) {
            loginAuditService.record(userRepository.findByUsername(request.getUsername()).orElse(null),
                    request.getUsername(), LoginAuditEventType.LOGIN_FAILURE, "Account is locked");
            throw ex;
        }

        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (user.getFailedLoginAttempts() != null && user.getFailedLoginAttempts() != 0) {
            user.setFailedLoginAttempts(0);
            userRepository.save(user);
        }
        loginAuditService.record(user, user.getUsername(), LoginAuditEventType.LOGIN_SUCCESS, null);

        String accessToken = jwtService.generateToken(user);
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(user);
        log.info("AuthServiceImpl:login :: SUCCESS username={} role={}", user.getUsername(), user.getRole());
        return buildTokenResponse(user, accessToken, refreshToken.getToken());
    }

    /** Increments the failed-attempt counter and auto-locks once MAX_FAILED_LOGIN_ATTEMPTS
     *  is reached — the account-lock policy from the spec, fully configurable via Settings. */
    private void recordFailedAttempt(String username) {
        Optional<User> userOpt = userRepository.findByUsername(username);
        if (userOpt.isEmpty()) {
            loginAuditService.record(null, username, LoginAuditEventType.LOGIN_FAILURE, "Unknown username");
            return;
        }
        User user = userOpt.get();
        int attempts = (user.getFailedLoginAttempts() == null ? 0 : user.getFailedLoginAttempts()) + 1;
        user.setFailedLoginAttempts(attempts);

        int maxAttempts = settingsService.getIntValue(SettingKey.MAX_FAILED_LOGIN_ATTEMPTS);
        if (attempts >= maxAttempts && user.getStatus() != User.UserStatus.LOCKED) {
            user.setStatus(User.UserStatus.LOCKED);
            user.setLockedAt(LocalDateTime.now());
            userRepository.save(user);
            loginAuditService.record(user, username, LoginAuditEventType.ACCOUNT_LOCKED,
                    "Locked after " + attempts + " consecutive failed attempts");
            log.warn("AuthServiceImpl:recordFailedAttempt :: username={} LOCKED after {} attempts", username, attempts);
            return;
        }
        userRepository.save(user);
        loginAuditService.record(user, username, LoginAuditEventType.LOGIN_FAILURE,
                "Attempt " + attempts + "/" + maxAttempts);
    }

    @Override
    public TokenResponse refreshToken(String token) {
        log.debug("AuthServiceImpl:refreshToken :: Processing refresh token");
        RefreshToken rt = refreshTokenService.verifyRefreshToken(token);
        User user = rt.getUser();
        String accessToken = jwtService.generateToken(user);
        String newRefreshToken = refreshTokenService.createRefreshToken(user).getToken();
        log.info("AuthServiceImpl:refreshToken :: SUCCESS username={}", user.getUsername());
        return buildTokenResponse(user, accessToken, newRefreshToken);
    }

    @Override
    @Transactional
    public void logout(String username) {
        log.info("AuthServiceImpl:logout :: username={}", username);
        userRepository.findByUsername(username).ifPresent(user -> {
            refreshTokenService.revokeByUserId(user.getId());
            loginAuditService.record(user, username, LoginAuditEventType.LOGOUT, null);
        });
    }

    @Override
    @Transactional
    public void changePassword(String username, ChangePasswordRequest request) {
        log.info("AuthServiceImpl:changePassword :: username={}", username);
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
            log.warn("AuthServiceImpl:changePassword :: Current password mismatch for username={}", username);
            throw new BusinessException("Current password is incorrect");
        }

        passwordPolicyService.validate(request.getNewPassword());
        passwordPolicyService.validateNotReused(user.getId(), request.getNewPassword());

        String encoded = passwordEncoder.encode(request.getNewPassword());
        user.setPasswordHash(encoded);
        user.setMustChangePassword(false);
        userRepository.save(user);
        passwordPolicyService.recordPasswordChange(user, encoded);
        refreshTokenService.revokeByUserId(user.getId());
        loginAuditService.record(user, username, LoginAuditEventType.PASSWORD_CHANGE, null);
        log.info("AuthServiceImpl:changePassword :: SUCCESS username={}", username);
    }

    @Override
    public UserProfileResponse getProfile(String username) {
        log.debug("AuthServiceImpl:getProfile :: username={}", username);
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        String employeeCategory = null;
        if (user.getEmployeeId() != null) {
            employeeCategory = employeeRepository.findById(user.getEmployeeId())
                    .map(e -> e.getEmployeeCategory().name()).orElse(null);
        }

        return UserProfileResponse.builder()
                .id(user.getId()).username(user.getUsername()).fullName(user.getFullName())
                .role(user.getRole().name()).employeeId(user.getEmployeeId()).status(user.getStatus().name())
                .mustChangePassword(user.getMustChangePassword())
                .employeeCategory(employeeCategory)
                .build();
    }

    @Override
    @Transactional
    public void forgotPassword(String email) {
        log.info("AuthServiceImpl:forgotPassword :: requested for email={}", email);
        Optional<Employee> employeeOpt = employeeRepository.findByEmail(email);
        if (employeeOpt.isEmpty()) {
            // Never reveal whether an email exists in the system.
            log.info("AuthServiceImpl:forgotPassword :: no employee found for email={} (silently ignored)", email);
            return;
        }
        Optional<User> userOpt = userRepository.findByEmployeeId(employeeOpt.get().getId());
        if (userOpt.isEmpty()) {
            log.info("AuthServiceImpl:forgotPassword :: employee has no linked user, email={} (silently ignored)", email);
            return;
        }
        User user = userOpt.get();
        Employee employee = employeeOpt.get();

        int expiryMinutes = settingsService.getIntValue(SettingKey.PASSWORD_RESET_TOKEN_EXPIRY_MINUTES);
        String token = UUID.randomUUID().toString();
        passwordResetTokenRepository.save(PasswordResetToken.builder()
                .user(user).token(token)
                .expiresAt(LocalDateTime.now().plusMinutes(expiryMinutes))
                .used(false).createdDate(LocalDateTime.now())
                .build());

        String resetLink = resetPasswordUrlBase + "?token=" + token;
        emailService.sendPasswordResetEmail(employee.getEmail(), employee.getName(), resetLink);
        loginAuditService.record(user, user.getUsername(), LoginAuditEventType.FORGOT_PASSWORD_REQUESTED, null);
        log.info("AuthServiceImpl:forgotPassword :: reset link issued for username={}", user.getUsername());
    }

    @Override
    @Transactional
    public void resetPassword(String token, String newPassword) {
        PasswordResetToken resetToken = passwordResetTokenRepository.findByToken(token)
                .orElseThrow(() -> new BusinessException("INVALID_RESET_TOKEN: this reset link is invalid"));
        if (Boolean.TRUE.equals(resetToken.getUsed())) {
            throw new BusinessException("RESET_TOKEN_ALREADY_USED: this reset link has already been used");
        }
        if (resetToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BusinessException("RESET_TOKEN_EXPIRED: this reset link has expired — request a new one");
        }

        User user = resetToken.getUser();
        passwordPolicyService.validate(newPassword);
        passwordPolicyService.validateNotReused(user.getId(), newPassword);

        String encoded = passwordEncoder.encode(newPassword);
        user.setPasswordHash(encoded);
        user.setMustChangePassword(false);
        user.setFailedLoginAttempts(0);
        if (user.getStatus() == User.UserStatus.LOCKED) {
            user.setStatus(User.UserStatus.ACTIVE);
            user.setLockedAt(null);
            loginAuditService.record(user, user.getUsername(), LoginAuditEventType.ACCOUNT_UNLOCKED,
                    "Unlocked via successful password reset");
        }
        userRepository.save(user);
        passwordPolicyService.recordPasswordChange(user, encoded);

        resetToken.setUsed(true);
        passwordResetTokenRepository.save(resetToken);

        refreshTokenService.revokeByUserId(user.getId());
        loginAuditService.record(user, user.getUsername(), LoginAuditEventType.PASSWORD_RESET, null);
        log.info("AuthServiceImpl:resetPassword :: SUCCESS username={}", user.getUsername());
    }

    private TokenResponse buildTokenResponse(User user, String accessToken, String refreshToken) {
        return TokenResponse.builder()
                .accessToken(accessToken).refreshToken(refreshToken).tokenType("Bearer")
                .username(user.getUsername()).fullName(user.getFullName()).role(user.getRole().name())
                .mustChangePassword(user.getMustChangePassword())
                .build();
    }
}