package com.business.erp.settings.service.impl;

import com.business.erp.settings.entity.AppSetting;
import com.business.erp.settings.entity.AppSettingSchedule;
import com.business.erp.settings.enums.SettingCategory;
import com.business.erp.settings.enums.SettingHistoryEventType;
import com.business.erp.settings.enums.SettingScheduleStatus;
import com.business.erp.settings.repository.AppSettingHistoryRepository;
import com.business.erp.settings.repository.AppSettingRepository;
import com.business.erp.settings.repository.AppSettingScheduleRepository;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

class SettingsActivationWorkerImplTest {

    @Test
    void activatesPendingScheduleOnce() {
        AppSettingScheduleRepository schedules = mock(AppSettingScheduleRepository.class);
        AppSettingRepository settings = mock(AppSettingRepository.class);
        AppSettingHistoryRepository history = mock(AppSettingHistoryRepository.class);
        Clock clock = Clock.fixed(Instant.parse("2026-08-06T00:00:00Z"), ZoneId.of("Asia/Kolkata"));
        SettingsActivationWorkerImpl worker = new SettingsActivationWorkerImpl(schedules, settings, history, clock);
        AppSettingSchedule schedule = AppSettingSchedule.builder().id(7L).settingKey("STANDARD_WORKING_HOURS")
                .scheduledValue("9").effectiveDate(LocalDate.of(2026, 8, 1)).status(SettingScheduleStatus.PENDING)
                .createdBy("admin").createdDate(LocalDate.of(2026, 7, 1).atStartOfDay()).build();
        AppSetting setting = AppSetting.builder().settingKey("STANDARD_WORKING_HOURS").settingValue("8")
                .settingCategory(SettingCategory.ATTENDANCE).dataType("INTEGER").editable(true)
                .effectiveFromDate(LocalDate.of(2026, 7, 1)).build();
        when(schedules.lockById(7L)).thenReturn(Optional.of(schedule));
        when(settings.lockBySettingKey("STANDARD_WORKING_HOURS")).thenReturn(Optional.of(setting));

        worker.activate(7L);

        assertEquals(SettingScheduleStatus.ACTIVATED, schedule.getStatus());
        assertEquals("9", setting.getSettingValue());
        verify(history).save(argThat(entry -> entry.getChangeType() == SettingHistoryEventType.ACTIVATED));

        worker.activate(7L);
        verify(settings, times(1)).save(setting);
    }
}
