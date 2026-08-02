package com.business.erp.auth.service.impl;

import com.business.erp.auth.entity.LoginAuditLog;
import com.business.erp.auth.entity.User;
import com.business.erp.auth.enums.LoginAuditEventType;
import com.business.erp.auth.repository.LoginAuditLogRepository;
import com.business.erp.auth.service.LoginAuditService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class LoginAuditServiceImpl implements LoginAuditService {

    private final LoginAuditLogRepository auditLogRepository;

    @Override
    @Transactional
    public void record(User user, String username, LoginAuditEventType eventType, String remarks) {
        auditLogRepository.save(LoginAuditLog.builder()
                .user(user).username(username).eventType(eventType)
                .eventDate(LocalDateTime.now()).remarks(remarks)
                .build());
    }
}