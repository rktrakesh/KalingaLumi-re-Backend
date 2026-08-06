package com.business.erp.settings.service;

/** Persists an activation failure after the attempted activation transaction rolls back. */
public interface SettingsActivationFailureRecorder {
    void recordFailure(Long scheduleId, String reason);
}
