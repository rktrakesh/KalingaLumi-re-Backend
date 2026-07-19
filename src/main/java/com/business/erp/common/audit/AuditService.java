package com.business.erp.common.audit;

public interface AuditService {
    void log(String module, String action, String entityType, Long entityId, Object oldValue, Object newValue);

    void log(String module, String action, String entityType, Long entityId);
}
