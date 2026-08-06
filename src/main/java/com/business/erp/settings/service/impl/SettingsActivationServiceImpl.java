package com.business.erp.settings.service.impl;

import com.business.erp.common.clock.ClockProvider;
import com.business.erp.settings.enums.SettingScheduleStatus;
import com.business.erp.settings.repository.AppSettingScheduleRepository;
import com.business.erp.settings.service.SettingsActivationFailureRecorder;
import com.business.erp.settings.service.SettingsActivationService;
import com.business.erp.settings.service.SettingsActivationWorker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class SettingsActivationServiceImpl implements SettingsActivationService {
    private final AppSettingScheduleRepository scheduleRepository;
    private final SettingsActivationWorker worker;
    private final SettingsActivationFailureRecorder failureRecorder;
    private final ClockProvider clockProvider;

    @Override
    public void activateDueSchedules() {
        scheduleRepository.findByStatusAndEffectiveDateLessThanEqual(SettingScheduleStatus.PENDING, clockProvider.today())
                .forEach(schedule -> activateSchedule(schedule.getId()));
    }

    @Override
    public void activateSchedule(Long scheduleId) {
        try {
            worker.activate(scheduleId);
        } catch (RuntimeException ex) {
            try {
                failureRecorder.recordFailure(scheduleId, ex.getMessage());
            } catch (RuntimeException failureRecordingException) {
                log.error("Settings schedule {} failed and its failure state could not be recorded", scheduleId,
                        failureRecordingException);
            }
            log.error("Settings schedule {} could not be activated", scheduleId, ex);
        }
    }
}
