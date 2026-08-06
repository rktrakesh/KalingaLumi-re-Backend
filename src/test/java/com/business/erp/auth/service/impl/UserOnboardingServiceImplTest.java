package com.business.erp.auth.service.impl;

import com.business.erp.auth.entity.User;
import com.business.erp.auth.repository.UserRepository;
import com.business.erp.auth.service.EmailService;
import com.business.erp.auth.service.PasswordPolicyService;
import com.business.erp.employee.entity.Employee;
import com.business.erp.settings.enums.SettingKey;
import com.business.erp.settings.service.SettingsService;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class UserOnboardingServiceImplTest {

    @Test
    void keepsUsernameCollisionSuffixBehaviorWithProvidedEmployeeSequence() {
        UserRepository users = mock(UserRepository.class);
        PasswordEncoder encoder = mock(PasswordEncoder.class);
        PasswordPolicyService passwords = mock(PasswordPolicyService.class);
        EmailService email = mock(EmailService.class);
        SettingsService settings = mock(SettingsService.class);
        UserOnboardingServiceImpl service = new UserOnboardingServiceImpl(users, encoder, passwords, email, settings);
        Employee employee = Employee.builder().name("Rakesh Kumar").email("rakesh@example.test")
                .joiningDate(LocalDate.of(2026, 8, 6)).build();
        when(settings.getCurrentValue(SettingKey.COMPANY_NAME)).thenReturn("Incense World");
        when(settings.getCurrentValue(SettingKey.COMPANY_SHORT_NAME)).thenReturn("IW");
        when(settings.getCurrentValue(SettingKey.USERNAME_GENERATION_RULE)).thenReturn("{CompanyShortName}_{FirstName}_{EmployeeNumber}");
        when(users.existsByUsername("iw_rakesh_0001")).thenReturn(true);
        when(users.existsByUsername("iw_rakesh_00011")).thenReturn(false);
        when(encoder.encode(any())).thenReturn("encoded");
        when(users.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        User created = service.onboard(employee, 1L, "admin");

        assertEquals("iw_rakesh_00011", created.getUsername());
        verify(users).existsByUsername("iw_rakesh_0001");
        verify(users).existsByUsername("iw_rakesh_00011");
    }
}
