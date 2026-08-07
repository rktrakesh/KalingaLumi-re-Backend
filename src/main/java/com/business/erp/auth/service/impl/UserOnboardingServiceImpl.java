package com.business.erp.auth.service.impl;

import com.business.erp.auth.entity.User;
import com.business.erp.auth.enums.LoginAuditEventType;
import com.business.erp.auth.repository.UserRepository;
import com.business.erp.auth.service.EmailService;
import com.business.erp.auth.service.LoginAuditService;
import com.business.erp.auth.service.LoginIdentifierService;
import com.business.erp.auth.service.PasswordPolicyService;
import com.business.erp.auth.service.UserOnboardingService;
import com.business.erp.auth.util.TemporaryPasswordGenerator;
import com.business.erp.common.clock.ClockProvider;
import com.business.erp.common.exception.BusinessException;
import com.business.erp.employee.entity.Employee;
import com.business.erp.settings.enums.SettingKey;
import com.business.erp.settings.service.IdentifierTemplateRenderer;
import com.business.erp.settings.service.IdentifierTemplateContext;
import com.business.erp.settings.service.SettingsService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class UserOnboardingServiceImpl implements UserOnboardingService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final PasswordPolicyService passwordPolicyService;
    private final EmailService emailService;
    private final LoginAuditService loginAuditService;
    private final LoginIdentifierService loginIdentifierService;
    private final SettingsService settingsService;
    private final ClockProvider clockProvider;
    private final Logger log = LoggerFactory.getLogger(UserOnboardingServiceImpl.class);

    @Override
    @Transactional
    public User onboard(Employee employee, long joiningSequence, String actor) {
        String username = generateUniqueUsername(employee, joiningSequence);
        String temporaryPassword = TemporaryPasswordGenerator.generate(
                settingsService.getIntValue(SettingKey.PASSWORD_MIN_LENGTH));
        passwordPolicyService.validate(temporaryPassword);
        String encodedPassword = passwordEncoder.encode(temporaryPassword);
        LocalDateTime issuedAt = clockProvider.now();

        userRepository.findByEmployee_Id(employee.getId()).ifPresent(existing -> {
            throw new BusinessException("Employee already has a login account");
        });

        // Default role is always the least-privileged ROLE_EMPLOYEE — Role controls
        // authorization, EmployeeCategory controls business behavior (Overtime/Payroll/
        // Performance already branch on category, not role). An admin can elevate the
        // role afterward via the existing UserManagementController if the new hire needs
        // MANAGER/SUPERVISOR/ADMIN access.
        User user = User.builder()
                .username(username).passwordHash(encodedPassword)
                .fullName(employee.getName())
                .role(User.Role.ROLE_EMPLOYEE)
                .roles(new LinkedHashSet<>(Set.of(User.Role.ROLE_EMPLOYEE)))
                .employee(employee)
                .status(User.UserStatus.ACTIVE)
                .mustChangePassword(true)
                .failedLoginAttempts(0)
                .credentialsExpired(false)
                .tokenVersion(0L)
                .temporaryPasswordIssuedAt(issuedAt)
                .temporaryPasswordExpiresAt(issuedAt.plusHours(
                        settingsService.getIntValue(SettingKey.TEMPORARY_PASSWORD_EXPIRY_HOURS)))
                .build();
        user.setCreatedBy(actor);
        user.setCreatedDate(issuedAt);
        loginIdentifierService.validateEmployeeLink(user, employee);

        User saved = userRepository.save(user);
        passwordPolicyService.recordPasswordChange(saved, encodedPassword);
        loginAuditService.record(saved, saved.getUsername(), actor,
                LoginAuditEventType.USER_CREATED, "User account created during employee onboarding");

        emailService.sendWelcomeEmail(employee.getEmail(), employee.getName(), username, temporaryPassword);

        log.info("UserOnboardingServiceImpl:onboard :: employeeId={} username={} by={}",
                employee.getId(), username, actor);
        return saved;
    }

    private String generateUniqueUsername(Employee employee, long joiningSequence) {
        IdentifierTemplateContext identifierContext = new IdentifierTemplateContext(
                settingsService.getCurrentValue(SettingKey.COMPANY_NAME),
                settingsService.getCurrentValue(SettingKey.COMPANY_SHORT_NAME),
                employee.getName(), joiningSequence, employee.getJoiningDate());
        String base = IdentifierTemplateRenderer.renderUsername(
                settingsService.getCurrentValue(SettingKey.USERNAME_GENERATION_RULE), identifierContext);
        String candidate = loginIdentifierService.normalizeUsername(base);
        int suffix = 1;
        while (!loginIdentifierService.isUsernameAvailable(candidate, null)) {
            candidate = loginIdentifierService.normalizeUsername(base + suffix);
            suffix++;
        }
        return candidate;
    }
}
