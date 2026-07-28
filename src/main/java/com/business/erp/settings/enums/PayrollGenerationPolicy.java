package com.business.erp.settings.enums;

/**
 * Controls whether payroll generation is restricted to after a period ends, or allowed
 * any time (starting in DRAFT). Stored as a company setting ({@code PAYROLL_GENERATION_POLICY})
 * and captured into the immutable {@link com.business.erp.payroll.entity.PayrollSettingsSnapshot}
 * at generation time, so a later policy change never affects already-generated payroll.
 */
public enum PayrollGenerationPolicy {
    /** Default — payroll for a period cannot be generated until that period has fully ended. */
    GENERATE_AFTER_PERIOD_END,
    /** Payroll may be generated at any time, starting in DRAFT status. */
    ALLOW_DRAFT_GENERATION
}