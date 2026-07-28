package com.business.erp.payroll.engine.audit;

import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;

/**
 * Serialized as the {@code newValue} JSON payload of an {@code AuditLog} row when payroll
 * generation is blocked by a validator. Recorded via
 * {@code AuditService} (which is {@code @Async} + {@code REQUIRES_NEW}), so it survives
 * even though the generation transaction itself rolls back.
 */
@Value
@Builder
public class PayrollGenerationBlockedEvent {
    String reason;
    String blockedByValidator;
    String policy;
    String requestedBy;
    LocalDateTime timestamp;
    int payrollMonth;
    int payrollYear;
}