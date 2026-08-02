package com.business.erp.auth.service;

import com.business.erp.auth.entity.User;
import com.business.erp.auth.enums.LoginAuditEventType;

public interface LoginAuditService {

    void record(User user, String username, LoginAuditEventType eventType, String remarks);
}