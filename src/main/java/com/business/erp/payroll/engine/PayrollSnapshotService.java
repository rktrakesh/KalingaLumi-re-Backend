package com.business.erp.payroll.engine;

import com.business.erp.payroll.entity.PayrollSettingsSnapshot;
import com.business.erp.payroll.engine.version.PayrollEngineVersion;
import com.business.erp.payroll.repository.PayrollSettingsSnapshotRepository;
import com.business.erp.settings.enums.PayrollGenerationPolicy;
import com.business.erp.settings.enums.SettingKey;
import com.business.erp.settings.service.SettingsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class PayrollSnapshotService {

    private final SettingsService settingsService;
    private final PayrollSettingsSnapshotRepository snapshotRepository;

    @Transactional
    public PayrollSettingsSnapshot captureForRun(Long payrollRunId, String capturedBy, PayrollGenerationPolicy generationPolicy) {
        PayrollSettingsSnapshot snapshot = PayrollSettingsSnapshot.builder()
                .payrollRunId(payrollRunId)
                .standardWorkingDays(settingsService.getIntValue(SettingKey.STANDARD_WORKING_DAYS))
                .workingHoursPerDay(settingsService.getIntValue(SettingKey.STANDARD_WORKING_HOURS))
                .paidLeavesPerMonth(settingsService.getIntValue(SettingKey.PAID_LEAVES_PER_MONTH))
                .overtimeMultiplier(settingsService.getDecimalValue(SettingKey.OVERTIME_MULTIPLIER))
                .weeklyOffMultiplier(settingsService.getDecimalValue(SettingKey.WEEKLY_OFF_MULTIPLIER))
                .holidayOtMultiplier(settingsService.getDecimalValue(SettingKey.HOLIDAY_OT_MULTIPLIER))
                .weeklyOffDays(settingsService.getCurrentValue(SettingKey.WEEKLY_OFF_DAYS))
                .leaveAllocationMethod(settingsService.getCurrentValue(SettingKey.LEAVE_ALLOCATION_METHOD))
                .unusedLeavePolicy(settingsService.getCurrentValue(SettingKey.UNUSED_PAID_LEAVE_POLICY))
                .leaveCarryForwardLimit(settingsService.getIntValue(SettingKey.LEAVE_CARRY_FORWARD_LIMIT))
                .leaveEncashmentEnabled(settingsService.getBooleanValue(SettingKey.LEAVE_ENCASHMENT_ENABLED))
                .generationPolicy(generationPolicy.name())
                .engineVersion(PayrollEngineVersion.CURRENT)
                .capturedBy(capturedBy)
                .capturedDate(LocalDateTime.now())
                .build();
        return snapshotRepository.save(snapshot);
    }

    public PayrollSettingsSnapshot getForRun(Long payrollRunId) {
        return snapshotRepository.findByPayrollRunId(payrollRunId)
                .orElseThrow(() -> new IllegalStateException(
                        "No settings snapshot found for payroll run " + payrollRunId + " — this should never happen."));
    }
}