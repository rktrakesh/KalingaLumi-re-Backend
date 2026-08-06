package com.business.erp.auth.service.impl;

import com.business.erp.auth.entity.User;
import com.business.erp.auth.repository.UserRepository;
import com.business.erp.auth.service.EmailService;
import com.business.erp.auth.service.PasswordPolicyService;
import com.business.erp.auth.service.UserOnboardingService;
import com.business.erp.auth.util.TemporaryPasswordGenerator;
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

@Service
@RequiredArgsConstructor
public class UserOnboardingServiceImpl implements UserOnboardingService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final PasswordPolicyService passwordPolicyService;
    private final EmailService emailService;
    private final SettingsService settingsService;
    private final Logger log = LoggerFactory.getLogger(UserOnboardingServiceImpl.class);

    @Override
    @Transactional
    public User onboard(Employee employee, long joiningSequence, String actor) {
        String username = generateUniqueUsername(employee, joiningSequence);
        String temporaryPassword = TemporaryPasswordGenerator.generate();
        String encodedPassword = passwordEncoder.encode(temporaryPassword);

        // Default role is always the least-privileged ROLE_EMPLOYEE — Role controls
        // authorization, EmployeeCategory controls business behavior (Overtime/Payroll/
        // Performance already branch on category, not role). An admin can elevate the
        // role afterward via the existing UserManagementController if the new hire needs
        // MANAGER/SUPERVISOR/ADMIN access.
        User user = User.builder()
                .username(username).passwordHash(encodedPassword)
                .fullName(employee.getName()).role(User.Role.ROLE_EMPLOYEE)
                .employee(employee)
                .status(User.UserStatus.ACTIVE)
                .mustChangePassword(true)
                .failedLoginAttempts(0)
                .build();
        user.setCreatedBy(actor);
        user.setCreatedDate(LocalDateTime.now());

        User saved = userRepository.save(user);
        passwordPolicyService.recordPasswordChange(saved, encodedPassword);

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
        String candidate = base;
        int suffix = 1;
        while (userRepository.existsByUsername(candidate)) {
            candidate = base + suffix;
            suffix++;
        }
        return candidate;
    }
}
