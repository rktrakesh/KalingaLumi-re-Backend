package com.business.erp.payroll.engine.context;

import com.business.erp.payroll.engine.period.PayrollPeriod;
import com.business.erp.payroll.entity.PayrollSettingsSnapshot;
import com.business.erp.settings.enums.PayrollGenerationPolicy;
import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;

/**
 * Everything a payroll generation validator (or, later, the generation orchestration
 * itself) needs, in one immutable object instead of a growing parameter list.
 * <p>
 * {@code snapshot} is {@code null} for the validators that run <em>before</em> the
 * settings snapshot is captured (which is all of them today — snapshot capture only
 * happens after every validator has passed). It exists on this context so that once a
 * snapshot has been captured, the same context shape can carry it forward to any
 * downstream consumer without inventing a second object.
 */
@Value
@Builder(toBuilder = true)
public class PayrollGenerationContext {
    PayrollPeriod period;
    PayrollSettingsSnapshot snapshot;
    String requestedBy;
    LocalDateTime generationDate;
    PayrollGenerationPolicy generationPolicy;
    String engineVersion;
    int calculationVersion;

    /** Returns a copy of this context with the snapshot filled in, once it has been captured. */
    public PayrollGenerationContext withSnapshot(PayrollSettingsSnapshot capturedSnapshot) {
        return this.toBuilder().snapshot(capturedSnapshot).build();
    }
}