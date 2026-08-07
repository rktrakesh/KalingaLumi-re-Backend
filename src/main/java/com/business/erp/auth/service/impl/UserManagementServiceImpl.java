package com.business.erp.auth.service.impl;

import com.business.erp.auth.dto.request.CreateUserRequest;
import com.business.erp.auth.dto.response.TemporaryPasswordResetResponse;
import com.business.erp.auth.entity.User;
import com.business.erp.auth.enums.LoginAuditEventType;
import com.business.erp.auth.repository.UserRepository;
import com.business.erp.auth.service.EmailService;
import com.business.erp.auth.service.LoginAuditService;
import com.business.erp.auth.service.LoginIdentifierService;
import com.business.erp.auth.service.PasswordPolicyService;
import com.business.erp.auth.service.RefreshTokenService;
import com.business.erp.auth.service.UserManagementService;
import com.business.erp.auth.util.TemporaryPasswordGenerator;
import com.business.erp.common.clock.ClockProvider;
import com.business.erp.common.exception.BusinessException;
import com.business.erp.common.exception.ResourceNotFoundException;
import com.business.erp.employee.entity.Employee;
import com.business.erp.employee.repository.EmployeeRepository;
import com.business.erp.settings.enums.SettingKey;
import com.business.erp.settings.service.SettingsService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class UserManagementServiceImpl implements UserManagementService {

    private final UserRepository userRepository;
    private final EmployeeRepository employeeRepository;
    private final RefreshTokenService refreshTokenService;
    private final LoginAuditService loginAuditService;
    private final LoginIdentifierService loginIdentifierService;
    private final PasswordEncoder passwordEncoder;
    private final PasswordPolicyService passwordPolicyService;
    private final SettingsService settingsService;
    private final EmailService emailService;
    private final ClockProvider clockProvider;

    @Override
    @Transactional
    public User create(CreateUserRequest request, String actor) {
        String username = loginIdentifierService.normalizeUsername(request.getUsername());
        loginIdentifierService.validateUsernameAvailable(username, null);
        passwordPolicyService.validate(request.getPassword());
        if (request.getFullName() == null || request.getFullName().trim().isEmpty()) {
            throw new BusinessException("Full name is required");
        }

        Employee employee = null;
        if (request.getEmployeeId() != null) {
            employee = loadEmployee(request.getEmployeeId());
            ensureEmployeeHasNoOtherAccount(request.getEmployeeId(), null);
        }

        User.Role requestedRole = request.getRole();
        if (requestedRole == null) {
            throw new BusinessException("A role is required");
        }

        Set<User.Role> roles = new LinkedHashSet<>();
        roles.add(requestedRole);
        LocalDateTime now = clockProvider.now();
        String passwordHash = passwordEncoder.encode(request.getPassword());

        User user = User.builder()
                .username(username)
                .fullName(request.getFullName().trim())
                .passwordHash(passwordHash)
                .roles(roles)
                .role(User.determineCompatibilityRole(roles))
                .employee(employee)
                .status(User.UserStatus.ACTIVE)
                .mustChangePassword(false)
                .failedLoginAttempts(0)
                .credentialsExpired(false)
                .tokenVersion(0L)
                .build();
        user.setCreatedBy(actor);
        user.setCreatedDate(now);

        if (employee != null) {
            loginIdentifierService.validateEmployeeLink(user, employee);
        }

        User saved = userRepository.save(user);
        passwordPolicyService.recordPasswordChange(saved, passwordHash);
        loginAuditService.record(saved, saved.getUsername(), actor,
                LoginAuditEventType.USER_CREATED, "User account created");
        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    public List<User> findAll() {
        return userRepository.findAllWithEmployee();
    }

    @Override
    @Transactional
    public User enable(Long userId, String actor) {
        User user = loadUser(userId);
        user.setStatus(User.UserStatus.ACTIVE);
        return secureChange(user, LoginAuditEventType.ACCOUNT_ENABLED, actor,
                "Enabled by administrator");
    }

    @Override
    @Transactional
    public User disable(Long userId, String actor) {
        User user = loadUser(userId);
        user.setStatus(User.UserStatus.INACTIVE);
        return secureChange(user, LoginAuditEventType.ACCOUNT_DISABLED, actor,
                "Disabled by administrator");
    }

    @Override
    @Transactional
    public User lock(Long userId, String actor) {
        User user = loadUser(userId);
        user.setStatus(User.UserStatus.LOCKED);
        user.setLockedAt(clockProvider.now());
        return secureChange(user, LoginAuditEventType.ACCOUNT_LOCKED, actor,
                "Locked by administrator");
    }

    @Override
    @Transactional
    public User unlock(Long userId, String actor) {
        User user = loadUser(userId);
        user.setStatus(User.UserStatus.ACTIVE);
        user.setLockedAt(null);
        user.setFailedLoginAttempts(0);
        return secureChange(user, LoginAuditEventType.ACCOUNT_UNLOCKED, actor,
                "Unlocked by administrator");
    }

    @Override
    @Transactional
    public TemporaryPasswordResetResponse resetTemporaryPassword(Long userId, String actor) {
        User user = loadUser(userId);
        String temporaryPassword = TemporaryPasswordGenerator.generate(
                settingsService.getIntValue(SettingKey.PASSWORD_MIN_LENGTH));
        passwordPolicyService.validate(temporaryPassword);
        passwordPolicyService.validateNotReused(user.getId(), temporaryPassword);
        String passwordHash = passwordEncoder.encode(temporaryPassword);
        LocalDateTime issuedAt = clockProvider.now();

        user.setPasswordHash(passwordHash);
        user.setMustChangePassword(true);
        user.setCredentialsExpired(false);
        user.setTemporaryPasswordIssuedAt(issuedAt);
        user.setTemporaryPasswordExpiresAt(issuedAt.plusHours(
                settingsService.getIntValue(SettingKey.TEMPORARY_PASSWORD_EXPIRY_HOURS)));

        User saved = secureChange(user, LoginAuditEventType.TEMPORARY_PASSWORD_RESET, actor,
                "Temporary password reset by administrator");
        passwordPolicyService.recordPasswordChange(saved, passwordHash);

        boolean emailAttempted = saved.getEmployee() != null
                && saved.getEmployee().getEmail() != null
                && !saved.getEmployee().getEmail().isBlank();
        if (emailAttempted) {
            emailService.sendWelcomeEmail(saved.getEmployee().getEmail(), saved.getFullName(),
                    saved.getUsername(), temporaryPassword);
        }

        return TemporaryPasswordResetResponse.builder()
                .temporaryPassword(temporaryPassword)
                .temporaryPasswordExpiresAt(saved.getTemporaryPasswordExpiresAt())
                .mustChangePassword(true)
                .emailDeliveryAttempted(emailAttempted)
                .build();
    }

    @Override
    @Transactional
    public User assignRole(Long userId, User.Role role, String actor) {
        User user = loadUser(userId);
        if (role == null) {
            throw new BusinessException("Role is required");
        }
        if (!user.getRoles().add(role)) {
            throw new BusinessException("Role is already assigned");
        }
        synchronizeCompatibilityRole(user);
        return secureChange(user, LoginAuditEventType.ROLE_ASSIGNED, actor,
                "Assigned role " + role.name());
    }

    @Override
    @Transactional
    public User removeRole(Long userId, User.Role role, String actor) {
        User user = loadUser(userId);
        if (role == null || !user.getRoles().contains(role)) {
            throw new BusinessException("Role is not assigned");
        }
        if (user.getRoles().size() == 1) {
            throw new BusinessException("A user must retain at least one role");
        }
        user.getRoles().remove(role);
        synchronizeCompatibilityRole(user);
        return secureChange(user, LoginAuditEventType.ROLE_REMOVED, actor,
                "Removed role " + role.name());
    }

    @Override
    @Transactional
    public User linkEmployee(Long userId, Long employeeId, String actor) {
        User user = loadUser(userId);
        Employee employee = loadEmployee(employeeId);
        ensureEmployeeHasNoOtherAccount(employeeId, userId);
        loginIdentifierService.validateEmployeeLink(user, employee);
        user.setEmployee(employee);
        return secureChange(user, LoginAuditEventType.EMPLOYEE_LINKED, actor,
                "Linked employee " + employeeId);
    }

    @Override
    @Transactional
    public User unlinkEmployee(Long userId, String actor) {
        User user = loadUser(userId);
        if (user.getEmployee() == null) {
            throw new BusinessException("User is not linked to an employee");
        }
        Long employeeId = user.getEmployee().getId();
        user.setEmployee(null);
        return secureChange(user, LoginAuditEventType.EMPLOYEE_UNLINKED, actor,
                "Unlinked employee " + employeeId);
    }

    @Override
    @Transactional
    public void invalidateSessionsForEmployee(Long employeeId, String actor, String reason) {
        userRepository.findByEmployee_Id(employeeId).ifPresent(user ->
                secureChange(user, LoginAuditEventType.EMPLOYEE_ACCESS_REVOKED,
                        actor, reason));
    }

    private User loadUser(Long userId) {
        return userRepository.findByIdWithEmployee(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));
    }

    private Employee loadEmployee(Long employeeId) {
        return employeeRepository.findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee", employeeId));
    }

    private void ensureEmployeeHasNoOtherAccount(Long employeeId, Long excludedUserId) {
        userRepository.findByEmployee_Id(employeeId)
                .filter(existing -> excludedUserId == null || !excludedUserId.equals(existing.getId()))
                .ifPresent(existing -> {
                    throw new BusinessException("Employee already has a login account");
                });
    }

    private User secureChange(User user, LoginAuditEventType eventType, String actor, String remarks) {
        user.setTokenVersion(currentTokenVersion(user) + 1);
        user.setUpdatedBy(actor);
        user.setUpdatedDate(clockProvider.now());
        User saved = userRepository.save(user);
        refreshTokenService.revokeByUserId(saved.getId());
        loginAuditService.record(saved, saved.getUsername(), actor, eventType, remarks);
        return saved;
    }

    private long currentTokenVersion(User user) {
        return user.getTokenVersion() == null ? 0L : user.getTokenVersion();
    }

    private void synchronizeCompatibilityRole(User user) {
        user.setRole(User.determineCompatibilityRole(user.getRoles()));
    }
}
