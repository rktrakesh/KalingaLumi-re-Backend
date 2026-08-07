package com.business.erp.settings.service.impl;

import com.business.erp.settings.entity.AppSettingHistory;
import com.business.erp.settings.entity.AppSettingSchedule;
import com.business.erp.settings.enums.SettingHistoryEventType;
import com.business.erp.settings.enums.SettingScheduleStatus;
import com.business.erp.settings.repository.AppSettingHistoryRepository;
import com.business.erp.settings.repository.AppSettingScheduleRepository;
import com.business.erp.settings.service.SettingsActivationFailureRecorder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class SettingsActivationFailureRecorderImpl implements SettingsActivationFailureRecorder {

    private static final int MAX_FAILURE_REASON_LENGTH = 500;

    private final AppSettingScheduleRepository scheduleRepository;
    private final AppSettingHistoryRepository historyRepository;
    private final Clock clock;

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordFailure(Long scheduleId, String reason) {
        AppSettingSchedule schedule = scheduleRepository.lockById(scheduleId).orElse(null);
        if (schedule == null || schedule.getStatus() != SettingScheduleStatus.PENDING) {
            return;
        }

        LocalDateTime occurredAt = LocalDateTime.now(clock);
        schedule.setStatus(SettingScheduleStatus.FAILED);
        schedule.setFailureReason(normalizeReason(reason));
        schedule.setUpdatedDate(occurredAt);
        scheduleRepository.save(schedule);

        historyRepository.save(AppSettingHistory.builder()
                .settingKey(schedule.getSettingKey())
                .changeType(SettingHistoryEventType.ACTIVATION_FAILED)
                .oldValue(null)
                .newValue(schedule.getScheduledValue())
                .effectiveFromDate(schedule.getEffectiveDate())
                .changedBy("SYSTEM")
                .changedDate(occurredAt)
                .build());
    }

    private String normalizeReason(String reason) {
        String value = reason == null || reason.isBlank() ? "Activation failed" : reason;
        return value.substring(0, Math.min(MAX_FAILURE_REASON_LENGTH, value.length()));
    }
}
