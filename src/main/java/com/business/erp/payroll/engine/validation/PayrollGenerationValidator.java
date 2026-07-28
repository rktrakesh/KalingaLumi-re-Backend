package com.business.erp.payroll.engine.validation;

import com.business.erp.payroll.engine.context.PayrollGenerationContext;

/**
 * One focused, single-responsibility check in the payroll generation validation pipeline.
 * Implementations throw {@code PayrollGenerationNotAllowedException} to block generation;
 * returning normally means "this validator has no objection". Order is controlled via
 * {@code @Order} on the implementing bean — {@link PayrollGenerationValidationPipeline}
 * runs them in that order and stops at the first failure.
 */
public interface PayrollGenerationValidator {
    void validate(PayrollGenerationContext context);
}