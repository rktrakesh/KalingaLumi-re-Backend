package com.business.erp.payroll.engine.version;

/**
 * Single source of truth for the payroll engine's version number. Stamped onto
 * {@code PayrollSettingsSnapshot}, {@code PayrollCalculationLog}, and generation-blocked
 * audit events, so historical payroll always knows exactly which engine version produced
 * it — a future formula change bumps this constant, never silently reinterprets old data.
 */
public final class PayrollEngineVersion {

    public static final String CURRENT = "1.0";

    private PayrollEngineVersion() {
    }
}