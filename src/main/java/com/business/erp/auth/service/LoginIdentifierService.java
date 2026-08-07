package com.business.erp.auth.service;

import com.business.erp.auth.entity.User;
import com.business.erp.employee.entity.Employee;

import java.util.Optional;

/**
 * Central policy for login identifiers. Usernames are stored in lowercase, employee
 * codes are stored in uppercase, and the two namespaces may never overlap.
 */
public interface LoginIdentifierService {

    String normalizeUsername(String username);

    String normalizeEmployeeCode(String employeeCode);

    Optional<User> findUnambiguousUser(String identifier);

    User loadForAuthentication(String identifier);

    boolean isUsernameAvailable(String username, Long excludedUserId);

    void validateUsernameAvailable(String username, Long excludedUserId);

    void validateEmployeeCodeAvailable(String employeeCode, Long excludedEmployeeId);

    void validateEmployeeLink(User user, Employee employee);
}
