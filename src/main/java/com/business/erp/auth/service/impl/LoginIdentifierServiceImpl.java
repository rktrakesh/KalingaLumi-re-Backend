package com.business.erp.auth.service.impl;

import com.business.erp.auth.entity.User;
import com.business.erp.auth.repository.UserRepository;
import com.business.erp.auth.service.LoginIdentifierService;
import com.business.erp.common.exception.BusinessException;
import com.business.erp.employee.entity.Employee;
import com.business.erp.employee.repository.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class LoginIdentifierServiceImpl implements LoginIdentifierService {

    private static final int USERNAME_MAX_LENGTH = 50;
    private static final int EMPLOYEE_CODE_MAX_LENGTH = 20;

    private final UserRepository userRepository;
    private final EmployeeRepository employeeRepository;

    @Override
    public String normalizeUsername(String username) {
        String normalized = requireText(username, "Username").toLowerCase(Locale.ROOT);
        if (normalized.length() > USERNAME_MAX_LENGTH) {
            throw new BusinessException("Username must not exceed " + USERNAME_MAX_LENGTH + " characters");
        }
        return normalized;
    }

    @Override
    public String normalizeEmployeeCode(String employeeCode) {
        String normalized = requireText(employeeCode, "Employee code").toUpperCase(Locale.ROOT);
        if (normalized.length() > EMPLOYEE_CODE_MAX_LENGTH) {
            throw new BusinessException(
                    "Employee code must not exceed " + EMPLOYEE_CODE_MAX_LENGTH + " characters");
        }
        return normalized;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<User> findUnambiguousUser(String identifier) {
        if (identifier == null || identifier.isBlank()) {
            return Optional.empty();
        }

        String trimmed = identifier.trim();
        List<User> usernameMatches = userRepository
                .findAllByUsernameIgnoreCaseWithEmployee(trimmed.toLowerCase(Locale.ROOT));
        List<User> employeeCodeMatches = userRepository
                .findAllByEmployeeCodeIgnoreCaseWithEmployee(trimmed.toUpperCase(Locale.ROOT));

        if (usernameMatches.size() > 1 || employeeCodeMatches.size() > 1
                || (!usernameMatches.isEmpty() && !employeeCodeMatches.isEmpty())) {
            return Optional.empty();
        }

        Map<Long, User> matches = new LinkedHashMap<>();
        usernameMatches.forEach(user -> matches.put(user.getId(), user));
        employeeCodeMatches.forEach(user -> matches.put(user.getId(), user));
        return matches.size() == 1 ? Optional.of(matches.values().iterator().next()) : Optional.empty();
    }

    @Override
    @Transactional(readOnly = true)
    public User loadForAuthentication(String identifier) {
        return findUnambiguousUser(identifier)
                .orElseThrow(() -> new UsernameNotFoundException("Invalid credentials"));
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isUsernameAvailable(String username, Long excludedUserId) {
        String normalized = normalizeUsername(username);
        boolean usernameTaken = userRepository.findAllByUsernameIgnoreCaseWithEmployee(normalized).stream()
                .anyMatch(user -> excludedUserId == null || !excludedUserId.equals(user.getId()));
        if (usernameTaken) {
            return false;
        }
        return employeeRepository.findAllByEmployeeCodeIgnoreCase(normalized).isEmpty();
    }

    @Override
    public void validateUsernameAvailable(String username, Long excludedUserId) {
        if (!isUsernameAvailable(username, excludedUserId)) {
            throw new BusinessException("Username is unavailable");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public void validateEmployeeCodeAvailable(String employeeCode, Long excludedEmployeeId) {
        String normalized = normalizeEmployeeCode(employeeCode);
        boolean employeeCodeTaken = employeeRepository.findAllByEmployeeCodeIgnoreCase(normalized).stream()
                .anyMatch(employee -> excludedEmployeeId == null || !excludedEmployeeId.equals(employee.getId()));
        if (employeeCodeTaken || userRepository.existsByUsernameIgnoreCase(normalized)) {
            throw new BusinessException("Employee code is unavailable");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public void validateEmployeeLink(User user, Employee employee) {
        if (user == null || employee == null) {
            throw new BusinessException("User and employee are required for account linking");
        }
        String normalizedCode = normalizeEmployeeCode(employee.getEmployeeCode());
        String normalizedUsername = normalizeUsername(user.getUsername());
        if (userRepository.existsByUsernameIgnoreCase(normalizedCode)
                || !employeeRepository.findAllByEmployeeCodeIgnoreCase(normalizedUsername).isEmpty()) {
            throw new BusinessException("Employee code conflicts with an existing username");
        }
    }

    private String requireText(String value, String label) {
        if (value == null || value.trim().isEmpty()) {
            throw new BusinessException(label + " is required");
        }
        return value.trim();
    }
}
