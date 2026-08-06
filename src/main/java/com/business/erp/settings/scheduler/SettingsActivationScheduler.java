package com.business.erp.settings.scheduler;

import com.business.erp.settings.service.SettingsActivationService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SettingsActivationScheduler {
    private final SettingsActivationService activationService;
    @Scheduled(cron = "0 */5 * * * *", zone = "${app.timezone:Asia/Kolkata}") public void activateDueSettings() { activationService.activateDueSchedules(); }
}
