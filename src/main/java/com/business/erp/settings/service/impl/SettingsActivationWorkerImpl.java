package com.business.erp.settings.service.impl;
import com.business.erp.settings.entity.*;
import com.business.erp.settings.enums.*;
import com.business.erp.settings.repository.*;
import com.business.erp.settings.service.SettingsActivationWorker;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class SettingsActivationWorkerImpl implements SettingsActivationWorker {

    private final AppSettingScheduleRepository scheduleRepository;
    private final AppSettingRepository settingRepository;
    private final AppSettingHistoryRepository historyRepository;
    private final Clock clock;

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void activate(Long scheduleId) {
        AppSettingSchedule schedule = scheduleRepository.lockById(scheduleId).orElse(null);
        if (schedule == null || schedule.getStatus() != SettingScheduleStatus.PENDING) {
            return;
        }

        AppSetting setting = settingRepository.lockBySettingKey(schedule.getSettingKey())
                .orElseThrow(() -> new IllegalStateException("Active setting not found: " + schedule.getSettingKey()));
        LocalDateTime activatedAt = LocalDateTime.now(clock);
        String oldValue = setting.getSettingValue();

        setting.setSettingValue(schedule.getScheduledValue());
        setting.setEffectiveFromDate(schedule.getEffectiveDate());
        settingRepository.save(setting);

        schedule.setStatus(SettingScheduleStatus.ACTIVATED);
        schedule.setFailureReason(null);
        // updatedDate is the lifecycle timestamp persisted by the current schedule schema.
        schedule.setUpdatedDate(activatedAt);
        scheduleRepository.save(schedule);

        historyRepository.save(AppSettingHistory.builder()
                .settingKey(schedule.getSettingKey())
                .changeType(SettingHistoryEventType.ACTIVATED)
                .oldValue(oldValue)
                .newValue(schedule.getScheduledValue())
                .effectiveFromDate(schedule.getEffectiveDate())
                .changedBy("SYSTEM")
                .changedDate(activatedAt)
                .build());
    }
}
