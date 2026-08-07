package com.business.erp.auth.service;

import com.business.erp.auth.entity.User;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.util.Set;

/**
 * Reusable ownership guard for existing endpoints that accept an employee id. It keeps
 * the backend authoritative without introducing a broader permission engine.
 */
@Service
public class AuthenticatedEmployeeAccessService {

    public Long requireAdminOrSelf(UserDetails principal, Long requestedEmployeeId) {
        return requireAuthorizedEmployee(principal, requestedEmployeeId, Set.of(User.Role.ROLE_ADMIN));
    }

    public Long requireAdminManagerOrSelf(UserDetails principal, Long requestedEmployeeId) {
        return requireAuthorizedEmployee(principal, requestedEmployeeId,
                Set.of(User.Role.ROLE_ADMIN, User.Role.ROLE_MANAGER));
    }

    public Long requireAdminSupervisorOrSelf(UserDetails principal, Long requestedEmployeeId) {
        return requireAuthorizedEmployee(principal, requestedEmployeeId,
                Set.of(User.Role.ROLE_ADMIN, User.Role.ROLE_SUPERVISOR));
    }

    private Long requireAuthorizedEmployee(UserDetails principal, Long requestedEmployeeId,
                                            Set<User.Role> broadAccessRoles) {
        if (!(principal instanceof User user)) {
            throw new AccessDeniedException("Employee identity is unavailable");
        }
        if (user.getRoles().stream().anyMatch(broadAccessRoles::contains)) {
            return requestedEmployeeId;
        }
        Long authenticatedEmployeeId = user.getEmployeeId();
        if (authenticatedEmployeeId == null || !authenticatedEmployeeId.equals(requestedEmployeeId)) {
            throw new AccessDeniedException("Access to the requested employee is denied");
        }
        return authenticatedEmployeeId;
    }
}
