package com.business.erp.auth.service;

import com.business.erp.auth.entity.User;

public interface PasswordPolicyService {

    void validate(String candidatePassword);

    void validateNotReused(Long userId, String candidatePassword);

    void recordPasswordChange(User user, String newPasswordHash);
}