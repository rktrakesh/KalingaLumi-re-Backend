package com.business.erp.payroll.engine.validation;

import com.business.erp.common.exception.PayrollGenerationNotAllowedException;
import com.business.erp.payroll.engine.context.PayrollGenerationContext;
import com.business.erp.settings.enums.SettingKey;
import com.business.erp.settings.service.SettingsService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Fails fast with a clear message if the settings a {@code PayrollSettingsSnapshot} would
 * be captured from are missing or nonsensical (e.g. zero working days, a negative
 * multiplier) — instead of the failure surfacing later as a division-by-zero or a silently
 * wrong payslip deep inside the calculation engine. Runs first, before anything about the
 * period or duplicates is even checked, since there is no point evaluating a period if the
 * numbers behind it can't produce a valid payroll anyway.
 */
@Component
@Order(1)
@RequiredArgsConstructor
public class SnapshotValidator implements PayrollGenerationValidator {

    private final SettingsService settingsService;

    @Override
    public void validate(PayrollGenerationContext context) {
        int standardWorkingDays = settingsService.getIntValue(SettingKey.STANDARD_WORKING_DAYS);
        int workingHoursPerDay = settingsService.getIntValue(SettingKey.STANDARD_WORKING_HOURS);
        int paidLeavesPerMonth = settingsService.getIntValue(SettingKey.PAID_LEAVES_PER_MONTH);
        BigDecimal overtimeMultiplier = settingsService.getDecimalValue(SettingKey.OVERTIME_MULTIPLIER);
        BigDecimal weeklyOffMultiplier = settingsService.getDecimalValue(SettingKey.WEEKLY_OFF_MULTIPLIER);
        BigDecimal holidayOtMultiplier = settingsService.getDecimalValue(SettingKey.HOLIDAY_OT_MULTIPLIER);
        String weeklyOffDays = settingsService.getCurrentValue(SettingKey.WEEKLY_OFF_DAYS);
        int carryForwardLimit = settingsService.getIntValue(SettingKey.LEAVE_CARRY_FORWARD_LIMIT);

        if (standardWorkingDays <= 0) {
            fail("STANDARD_WORKING_DAYS must be greater than zero (currently " + standardWorkingDays + ")");
        }
        if (workingHoursPerDay <= 0) {
            fail("STANDARD_WORKING_HOURS must be greater than zero (currently " + workingHoursPerDay + ")");
        }
        if (paidLeavesPerMonth < 0) {
            fail("PAID_LEAVES_PER_MONTH cannot be negative (currently " + paidLeavesPerMonth + ")");
        }
        if (overtimeMultiplier == null || overtimeMultiplier.compareTo(BigDecimal.ZERO) < 0) {
            fail("OVERTIME_MULTIPLIER is missing or negative");
        }
        if (weeklyOffMultiplier == null || weeklyOffMultiplier.compareTo(BigDecimal.ZERO) < 0) {
            fail("WEEKLY_OFF_MULTIPLIER is missing or negative");
        }
        if (holidayOtMultiplier == null || holidayOtMultiplier.compareTo(BigDecimal.ZERO) < 0) {
            fail("HOLIDAY_OT_MULTIPLIER is missing or negative");
        }
        if (weeklyOffDays == null || weeklyOffDays.isBlank()) {
            fail("WEEKLY_OFF_DAYS is not configured");
        }
        if (carryForwardLimit < 0) {
            fail("LEAVE_CARRY_FORWARD_LIMIT cannot be negative (currently " + carryForwardLimit + ")");
        }
    }

    private void fail(String reason) {
        throw new PayrollGenerationNotAllowedException("Payroll settings are invalid: " + reason);
    }
}