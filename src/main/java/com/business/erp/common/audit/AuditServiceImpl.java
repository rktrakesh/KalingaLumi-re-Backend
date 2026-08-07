package com.business.erp.common.audit;

import com.business.erp.common.clock.ClockProvider;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuditServiceImpl implements AuditService {

    private final AuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper;
    private final ClockProvider clockProvider;
    private final Logger log = LoggerFactory.getLogger(AuditServiceImpl.class);

    @Async
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void log(String module, String action, String entityType, Long entityId,
                    Object oldValue, Object newValue) {
        logAs(getCurrentUser(), module, action, entityType, entityId, oldValue, newValue);
    }

    @Async
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logAs(String username, String module, String action, String entityType, Long entityId,
                      Object oldValue, Object newValue) {
        log.debug("AuditServiceImpl:log :: module={} action={} entityType={} entityId={}",
                module, action, entityType, entityId);
        try {
            AuditLog entry = AuditLog.builder()
                    .username(username == null || username.isBlank() ? "SYSTEM" : username)
                    .module(module)
                    .action(action)
                    .entityType(entityType)
                    .entityId(entityId)
                    .oldValue(oldValue != null ? objectMapper.writeValueAsString(oldValue) : null)
                    .newValue(newValue != null ? objectMapper.writeValueAsString(newValue) : null)
                    .createdAt(clockProvider.now())
                    .build();
            auditLogRepository.save(entry);
            log.info("AuditServiceImpl:log :: SUCCESS module={} action={} entityId={}", module, action, entityId);
        } catch (JsonProcessingException e) {
            log.error("AuditServiceImpl:log :: ERROR serializing audit values for module={} action={} :: {}",
                    module, action, e.getMessage(), e);
        }
    }

    @Async
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void log(String module, String action, String entityType, Long entityId) {
        log(module, action, entityType, entityId, null, null);
    }

    private String getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return (auth != null && auth.isAuthenticated()) ? auth.getName() : "SYSTEM";
    }
}
