package com.business.erp.settings.service.impl;

import com.business.erp.common.clock.ClockProvider;
import com.business.erp.settings.entity.AppSettingSchedule;
import com.business.erp.settings.enums.SettingScheduleStatus;
import com.business.erp.settings.repository.AppSettingScheduleRepository;
import com.business.erp.settings.service.SettingsActivationFailureRecorder;
import com.business.erp.settings.service.SettingsActivationWorker;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.Mockito.*;

class SettingsActivationServiceImplTest {

    @Test
    void failureOfOneDueScheduleDoesNotBlockAnother() {
        AppSettingScheduleRepository schedules = mock(AppSettingScheduleRepository.class);
        SettingsActivationWorker worker = mock(SettingsActivationWorker.class);
        SettingsActivationFailureRecorder failureRecorder = mock(SettingsActivationFailureRecorder.class);
        ClockProvider clockProvider = mock(ClockProvider.class);
        SettingsActivationServiceImpl service = new SettingsActivationServiceImpl(schedules, worker, failureRecorder, clockProvider);
        when(clockProvider.today()).thenReturn(LocalDate.of(2026, 8, 6));
        when(schedules.findByStatusAndEffectiveDateLessThanEqual(eq(SettingScheduleStatus.PENDING), any()))
                .thenReturn(List.of(AppSettingSchedule.builder().id(1L).build(), AppSettingSchedule.builder().id(2L).build()));
        doThrow(new IllegalStateException("broken setting")).when(worker).activate(1L);

        service.activateDueSchedules();

        verify(failureRecorder).recordFailure(1L, "broken setting");
        verify(worker).activate(2L);
    }
}
