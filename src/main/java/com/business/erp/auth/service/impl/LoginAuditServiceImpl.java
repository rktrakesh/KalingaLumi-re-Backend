package com.business.erp.auth.service.impl;

import com.business.erp.auth.entity.LoginAuditLog;
import com.business.erp.auth.entity.User;
import com.business.erp.auth.enums.LoginAuditEventType;
import com.business.erp.auth.repository.LoginAuditLogRepository;
import com.business.erp.auth.service.IamRequestContextResolver;
import com.business.erp.auth.service.LoginAuditService;
import com.business.erp.common.clock.ClockProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class LoginAuditServiceImpl implements LoginAuditService {

    private final LoginAuditLogRepository auditLogRepository;
    private final IamRequestContextResolver requestContextResolver;
    private final ClockProvider clockProvider;

    @Override
    @Transactional
    public void record(User targetUser, String targetUsername, String actorUsername,
                       LoginAuditEventType eventType, String remarks) {
        IamRequestContextResolver.RequestContext context = requestContextResolver.resolve();
        auditLogRepository.save(LoginAuditLog.builder()
                .user(targetUser)
                .username(limit(targetUsername == null ? "UNKNOWN" : targetUsername, 50))
                .actorUsername(limit(actorUsername, 50))
                .eventType(eventType)
                .eventDate(clockProvider.now())
                .ipAddress(context.ipAddress())
                .userAgent(context.userAgent())
                .device(context.device())
                .remarks(limit(remarks, 255))
                .build());
    }

    private String limit(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        String sanitized = value.replace("\r", "").replace("\n", "").trim();
        return sanitized.substring(0, Math.min(maxLength, sanitized.length()));
    }
}
