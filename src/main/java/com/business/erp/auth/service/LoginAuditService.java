package com.business.erp.auth.service;

import com.business.erp.auth.entity.User;
import com.business.erp.auth.enums.LoginAuditEventType;

public interface LoginAuditService {

    default void record(User user, String username, LoginAuditEventType eventType, String remarks) {
        record(user, username, username, eventType, remarks);
    }

    void record(User targetUser, String targetUsername, String actorUsername,
                LoginAuditEventType eventType, String remarks);
}
