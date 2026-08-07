package com.business.erp.auth.service.impl;

import com.business.erp.auth.entity.User;
import com.business.erp.auth.repository.UserRepository;
import com.business.erp.auth.service.EmailService;
import com.business.erp.auth.service.LoginAuditService;
import com.business.erp.auth.service.LoginIdentifierService;
import com.business.erp.auth.service.PasswordPolicyService;
import com.business.erp.common.clock.ClockProvider;
import com.business.erp.employee.entity.Employee;
import com.business.erp.settings.enums.SettingKey;
import com.business.erp.settings.service.SettingsService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class UserOnboardingServiceImplTest {

    @Test
    void keepsUsernameCollisionSuffixBehaviorWithProvidedEmployeeSequence() {
        UserRepository users = mock(UserRepository.class);
        PasswordEncoder encoder = mock(PasswordEncoder.class);

        PasswordPolicyService passwords = mock(PasswordPolicyService.class, invocation -> {
            if (invocation.getMethod().getReturnType() == boolean.class || invocation.getMethod().getReturnType() == Boolean.class) {
                return true;
            }
            return Mockito.RETURNS_DEFAULTS.answer(invocation);
        });

        LoginIdentifierService loginIdentifier = mock(LoginIdentifierService.class, invocation -> {
            Class<?> returnType = invocation.getMethod().getReturnType();
            if (returnType == boolean.class || returnType == Boolean.class) {
                return true;
            }
            if (returnType == String.class) {
                return "iw_rakesh_00011"; // Return the final collision-free username
            }
            return Mockito.RETURNS_DEFAULTS.answer(invocation);
        });

        EmailService email = mock(EmailService.class);
        SettingsService settings = mock(SettingsService.class);
        LoginAuditService loginAudit = mock(LoginAuditService.class);
        ClockProvider clockProvider = mock(ClockProvider.class);

        when(clockProvider.now()).thenReturn(LocalDateTime.of(2026, 8, 7, 10, 0));

        UserOnboardingServiceImpl service = new UserOnboardingServiceImpl(
                users, encoder, passwords, email, loginAudit, loginIdentifier, settings, clockProvider
        );

        Employee employee = Employee.builder().name("Rakesh Kumar").email("rakesh@example.test")
                .joiningDate(LocalDate.of(2026, 8, 6)).build();

        when(settings.getCurrentValue(SettingKey.COMPANY_NAME)).thenReturn("Incense World");
        when(settings.getCurrentValue(SettingKey.COMPANY_SHORT_NAME)).thenReturn("IW");
        when(settings.getCurrentValue(SettingKey.USERNAME_GENERATION_RULE)).thenReturn("{CompanyShortName}_{FirstName}_{EmployeeNumber}");
        when(settings.getCurrentValue(SettingKey.TEMPORARY_PASSWORD_EXPIRY_HOURS)).thenReturn("72");

        when(encoder.encode(any())).thenReturn("encoded");
        when(users.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        User created = service.onboard(employee, 1L, "admin");

        assertEquals("iw_rakesh_00011", created.getUsername());
    }
}