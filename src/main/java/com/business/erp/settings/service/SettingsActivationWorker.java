package com.business.erp.settings.service;

/** Activates one schedule in an independent transaction. */
public interface SettingsActivationWorker {
    void activate(Long scheduleId);
}
