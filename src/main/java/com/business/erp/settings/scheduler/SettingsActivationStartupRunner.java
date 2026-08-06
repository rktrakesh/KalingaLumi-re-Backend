package com.business.erp.settings.scheduler;

import com.business.erp.settings.service.SettingsActivationService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SettingsActivationStartupRunner {
    private final SettingsActivationService activationService;
    @PostConstruct void recoverDueSchedules() { activationService.activateDueSchedules(); }
}
