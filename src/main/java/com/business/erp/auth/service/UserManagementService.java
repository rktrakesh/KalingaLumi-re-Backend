package com.business.erp.auth.service;

import com.business.erp.auth.dto.request.CreateUserRequest;
import com.business.erp.auth.dto.response.TemporaryPasswordResetResponse;
import com.business.erp.auth.entity.User;

import java.util.List;

/** Reusable administrative lifecycle boundary; controllers contain no account-security rules. */
public interface UserManagementService {
    User create(CreateUserRequest request, String actor);

    List<User> findAll();

    User enable(Long userId, String actor);

    User disable(Long userId, String actor);

    User lock(Long userId, String actor);

    User unlock(Long userId, String actor);

    TemporaryPasswordResetResponse resetTemporaryPassword(Long userId, String actor);

    User assignRole(Long userId, User.Role role, String actor);

    User removeRole(Long userId, User.Role role, String actor);

    User linkEmployee(Long userId, Long employeeId, String actor);

    User unlinkEmployee(Long userId, String actor);

    void invalidateSessionsForEmployee(Long employeeId, String actor, String reason);
}
