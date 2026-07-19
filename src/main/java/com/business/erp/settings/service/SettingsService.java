package com.business.erp.settings.service;

import com.business.erp.settings.entity.AppSetting;
import com.business.erp.settings.entity.AppSettingHistory;
import com.business.erp.settings.enums.SettingKey;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface SettingsService {
    String getCurrentValue(SettingKey key);

    int getIntValue(SettingKey key);

    BigDecimal getDecimalValue(SettingKey key);

    boolean getBooleanValue(SettingKey key);

    String getValueAsOf(SettingKey key, LocalDate asOfDate);

    int getIntValueAsOf(SettingKey key, LocalDate asOfDate);

    BigDecimal getDecimalValueAsOf(SettingKey key, LocalDate asOfDate);

    AppSetting updateSetting(SettingKey key, String newValue, String changedBy);

    List<AppSetting> getAllSettings();

    List<AppSettingHistory> getHistory(SettingKey key);
}
