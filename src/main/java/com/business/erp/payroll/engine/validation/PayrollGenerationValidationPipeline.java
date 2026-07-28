package com.business.erp.payroll.engine.validation;

import com.business.erp.common.audit.AuditService;
import com.business.erp.common.exception.PayrollGenerationNotAllowedException;
import com.business.erp.payroll.engine.audit.PayrollGenerationBlockedEvent;
import com.business.erp.payroll.engine.context.PayrollGenerationContext;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Runs every registered {@link PayrollGenerationValidator} in {@code @Order} sequence and
 * stops at the first failure. On failure, records a "Payroll Generation Blocked" audit
 * event before rethrowing — via {@code AuditService}, which is {@code @Async} +
 * {@code REQUIRES_NEW}, so the audit row survives even though the caller's generation
 * transaction rolls back and nothing else was ever persisted.
 */
@Component
@RequiredArgsConstructor
public class PayrollGenerationValidationPipeline {

    private final List<PayrollGenerationValidator> validators;
    private final AuditService auditService;
    private final Logger log = LoggerFactory.getLogger(PayrollGenerationValidationPipeline.class);

    public void validate(PayrollGenerationContext context) {
        log.info("PayrollGenerationValidationPipeline:validate :: Validation Started period={} policy={}",
                context.getPeriod(), context.getGenerationPolicy());

        for (PayrollGenerationValidator validator : validators) {
            try {
                validator.validate(context);
            } catch (PayrollGenerationNotAllowedException ex) {
                String validatorName = validator.getClass().getSimpleName();
                log.warn("PayrollGenerationValidationPipeline:validate :: Validation Failed validator={} reason={}",
                        validatorName, ex.getMessage());
                recordBlockedAudit(context, ex.getMessage(), validatorName);
                log.info("PayrollGenerationValidationPipeline:validate :: Payroll Generation Blocked period={}", context.getPeriod());
                throw ex;
            }
        }

        log.info("PayrollGenerationValidationPipeline:validate :: Validation Passed period={} — Payroll Generation Allowed", context.getPeriod());
    }

    private void recordBlockedAudit(PayrollGenerationContext context, String reason, String validatorName) {
        PayrollGenerationBlockedEvent event = PayrollGenerationBlockedEvent.builder()
                .reason(reason)
                .blockedByValidator(validatorName)
                .policy(context.getGenerationPolicy() != null ? context.getGenerationPolicy().name() : null)
                .requestedBy(context.getRequestedBy())
                .timestamp(context.getGenerationDate())
                .payrollMonth(context.getPeriod().getMonth())
                .payrollYear(context.getPeriod().getYear())
                .build();
        auditService.log("PAYROLL", "Payroll Generation Blocked", "PayrollPeriod", null, null, event);
    }
}