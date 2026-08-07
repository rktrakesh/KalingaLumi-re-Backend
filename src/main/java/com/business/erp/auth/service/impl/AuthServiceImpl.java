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
import com.business.erp.common.clock.ClockProvider;
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
import org.springframework.security.authentication.CredentialsExpiredException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.LinkedHashSet;
import java.util.stream.Collectors;
import java.util.UUID;

/**
 * Login Eligibility (employee status ACTIVE/ON_NOTICE) is deliberately NOT checked here
 * anymore — see {@link User#isEnabled()} and {@code UserDetailsServiceImpl}. This class no
 * longer queries {@code EmployeeRepository} to verify login eligibility; the only remaining
 * uses of it below ({@link #getProfile}, {@link #forgotPassword}) are unrelated lookups
 * (reading the employee category for the profile response, and finding an employee by
 * email for the reset flow) that have nothing to do with authentication itself.
 */
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
    private final LoginIdentifierService loginIdentifierService;
    private final ClockProvider clockProvider;
    private final Logger log = LoggerFactory.getLogger(AuthServiceImpl.class);

    @Value("${app.frontend.reset-password-url:http://localhost:9999/reset-password}")
    private String resetPasswordUrlBase;

    @Override
    @Transactional(noRollbackFor = {
            BadCredentialsException.class,
            LockedException.class,
            DisabledException.class,
            CredentialsExpiredException.class,
            BusinessException.class
    })
    public TokenResponse login(LoginRequest request) {
        log.info("AuthServiceImpl:login :: Attempting login");
        Authentication authResult;
        try {
            authResult = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getUsername().trim(), request.getPassword()));
        } catch (BadCredentialsException ex) {
            recordFailedAttempt(request.getUsername());
            throw new BadCredentialsException("Invalid credentials");
        } catch (LockedException ex) {
            auditLoginFailure(request.getUsername(), "Account is locked");
            throw new BadCredentialsException("Invalid credentials");
        } catch (DisabledException ex) {
            auditLoginFailure(request.getUsername(), "Account is not login-eligible");
            throw new BadCredentialsException("Invalid credentials");
        } catch (CredentialsExpiredException ex) {
            auditLoginFailure(request.getUsername(), "Credentials have expired");
            throw new BadCredentialsException("Invalid credentials");
        }

        User user = (User) authResult.getPrincipal();

        if (Boolean.TRUE.equals(user.getMustChangePassword()) && user.getTemporaryPasswordExpiresAt() != null
                && !user.getTemporaryPasswordExpiresAt().isAfter(clockProvider.now())) {
            user.setCredentialsExpired(true);
            invalidateTokens(user, user.getUsername());
            userRepository.save(user);
            loginAuditService.record(user, user.getUsername(), LoginAuditEventType.LOGIN_FAILURE,
                    "Temporary password expired");
            throw new BusinessException("TEMPORARY_PASSWORD_EXPIRED: use Forgot Password to set a new password");
        }

        if (user.getFailedLoginAttempts() != null && user.getFailedLoginAttempts() != 0) {
            user.setFailedLoginAttempts(0);
        }
        user.setLastLoginAt(clockProvider.now());
        userRepository.save(user);
        loginAuditService.record(user, user.getUsername(), LoginAuditEventType.LOGIN_SUCCESS, null);

        String accessToken = jwtService.generateToken(user);
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(user);
        log.info("AuthServiceImpl:login :: SUCCESS username={} role={}", user.getUsername(), user.getRole());
        return buildTokenResponse(user, accessToken, refreshToken.getToken());
    }

    /** Increments the failed-attempt counter and auto-locks once MAX_FAILED_LOGIN_ATTEMPTS
     *  is reached — the account-lock policy from the spec, fully configurable via Settings. */
    private void recordFailedAttempt(String identifier) {
        Optional<User> userOpt = loginIdentifierService.findUnambiguousUser(identifier);
        if (userOpt.isEmpty()) {
            loginAuditService.record(null, auditIdentifier(identifier),
                    LoginAuditEventType.LOGIN_FAILURE, "Invalid credentials");
            return;
        }
        User user = userOpt.get();
        int attempts = (user.getFailedLoginAttempts() == null ? 0 : user.getFailedLoginAttempts()) + 1;
        user.setFailedLoginAttempts(attempts);

        int maxAttempts = settingsService.getIntValue(SettingKey.MAX_FAILED_LOGIN_ATTEMPTS);
        if (attempts >= maxAttempts && user.getStatus() != User.UserStatus.LOCKED) {
            user.setStatus(User.UserStatus.LOCKED);
            user.setLockedAt(clockProvider.now());
            invalidateTokens(user, user.getUsername());
            userRepository.save(user);
            loginAuditService.record(user, user.getUsername(), LoginAuditEventType.ACCOUNT_LOCKED,
                    "Locked after " + attempts + " consecutive failed attempts");
            log.warn("AuthServiceImpl:recordFailedAttempt :: userId={} locked after {} attempts",
                    user.getId(), attempts);
            return;
        }
        userRepository.save(user);
        loginAuditService.record(user, user.getUsername(), LoginAuditEventType.LOGIN_FAILURE,
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
        userRepository.findByUsernameIgnoreCase(username.trim()).ifPresent(user -> {
            invalidateTokens(user, username);
            userRepository.save(user);
            loginAuditService.record(user, username, LoginAuditEventType.LOGOUT, null);
        });
    }

    @Override
    @Transactional
    public TokenResponse changePassword(String username, ChangePasswordRequest request) {
        log.info("AuthServiceImpl:changePassword :: username={}", username);
        User user = userRepository.findByUsernameIgnoreCase(username.trim())
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
        user.setCredentialsExpired(false);
        user.setTemporaryPasswordIssuedAt(null);
        user.setTemporaryPasswordExpiresAt(null);
        invalidateTokens(user, username);
        userRepository.save(user);
        passwordPolicyService.recordPasswordChange(user, encoded);
        loginAuditService.record(user, username, LoginAuditEventType.PASSWORD_CHANGE, null);

        String accessToken = jwtService.generateToken(user);
        String refreshToken = refreshTokenService.createRefreshToken(user).getToken();
        log.info("AuthServiceImpl:changePassword :: SUCCESS username={}", username);
        return buildTokenResponse(user, accessToken, refreshToken);
    }

    @Override
    @Transactional(readOnly = true)
    public UserProfileResponse getProfile(String username) {
        log.debug("AuthServiceImpl:getProfile :: username={}", username);
        User user = userRepository.findByUsernameIgnoreCase(username.trim())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        String employeeCategory = null;
        if (user.getEmployeeId() != null) {
            employeeCategory = employeeRepository.findById(user.getEmployeeId())
                    .map(e -> e.getEmployeeCategory().getCode()).orElse(null);
        }

        return UserProfileResponse.builder()
                .id(user.getId()).username(user.getUsername()).fullName(user.getFullName())
                .role(user.getRole().name()).employeeId(user.getEmployeeId()).status(user.getStatus().name())
                .roles(roleNames(user))
                .mustChangePassword(user.getMustChangePassword())
                .credentialsExpired(user.getCredentialsExpired())
                .failedLoginAttempts(user.getFailedLoginAttempts()).lockedAt(user.getLockedAt())
                .lastLoginAt(user.getLastLoginAt())
                .temporaryPasswordIssuedAt(user.getTemporaryPasswordIssuedAt())
                .temporaryPasswordExpiresAt(user.getTemporaryPasswordExpiresAt())
                .enabled(user.isEnabled())
                .accountNonLocked(user.isAccountNonLocked())
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
        Optional<User> userOpt = userRepository.findByEmployee_Id(employeeOpt.get().getId());
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
                .expiresAt(clockProvider.now().plusMinutes(expiryMinutes))
                .used(false).createdDate(clockProvider.now())
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
        if (!resetToken.getExpiresAt().isAfter(clockProvider.now())) {
            throw new BusinessException("RESET_TOKEN_EXPIRED: this reset link has expired — request a new one");
        }

        User user = resetToken.getUser();
        passwordPolicyService.validate(newPassword);
        passwordPolicyService.validateNotReused(user.getId(), newPassword);

        String encoded = passwordEncoder.encode(newPassword);
        user.setPasswordHash(encoded);
        user.setMustChangePassword(false);
        user.setCredentialsExpired(false);
        user.setTemporaryPasswordIssuedAt(null);
        user.setTemporaryPasswordExpiresAt(null);
        user.setFailedLoginAttempts(0);
        if (user.getStatus() == User.UserStatus.LOCKED) {
            user.setStatus(User.UserStatus.ACTIVE);
            user.setLockedAt(null);
            loginAuditService.record(user, user.getUsername(), LoginAuditEventType.ACCOUNT_UNLOCKED,
                    "Unlocked via successful password reset");
        }
        invalidateTokens(user, user.getUsername());
        userRepository.save(user);
        passwordPolicyService.recordPasswordChange(user, encoded);

        resetToken.setUsed(true);
        passwordResetTokenRepository.save(resetToken);

        loginAuditService.record(user, user.getUsername(), LoginAuditEventType.PASSWORD_RESET, null);
        log.info("AuthServiceImpl:resetPassword :: SUCCESS username={}", user.getUsername());
    }

    private TokenResponse buildTokenResponse(User user, String accessToken, String refreshToken) {
        return TokenResponse.builder()
                .accessToken(accessToken).refreshToken(refreshToken).tokenType("Bearer")
                .username(user.getUsername()).fullName(user.getFullName()).role(user.getRole().name())
                .roles(roleNames(user))
                .mustChangePassword(user.getMustChangePassword())
                .build();
    }

    private void auditLoginFailure(String identifier, String remarks) {
        User user = loginIdentifierService.findUnambiguousUser(identifier).orElse(null);
        loginAuditService.record(user, user == null ? auditIdentifier(identifier) : user.getUsername(),
                LoginAuditEventType.LOGIN_FAILURE, remarks);
    }

    private String auditIdentifier(String identifier) {
        if (identifier == null || identifier.isBlank()) {
            return "UNKNOWN";
        }
        String trimmed = identifier.trim();
        return trimmed.substring(0, Math.min(50, trimmed.length()));
    }

    private void invalidateTokens(User user, String actor) {
        long currentVersion = user.getTokenVersion() == null ? 0L : user.getTokenVersion();
        user.setTokenVersion(currentVersion + 1);
        user.setUpdatedBy(actor);
        user.setUpdatedDate(clockProvider.now());
        refreshTokenService.revokeByUserId(user.getId());
    }

    private java.util.Set<String> roleNames(User user) {
        return user.getRoles().stream()
                .sorted()
                .map(Enum::name)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }
}
