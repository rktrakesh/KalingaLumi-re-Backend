package com.business.erp.settings.service;
/** Coordinates due schedules; individual activation is delegated to a separate transactional worker. */
public interface SettingsActivationService {
    void activateDueSchedules();
    void activateSchedule(Long scheduleId);
}
