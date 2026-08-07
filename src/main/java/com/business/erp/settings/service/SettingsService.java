package com.business.erp.settings.service;

import com.business.erp.settings.entity.AppSetting;
import com.business.erp.settings.entity.AppSettingHistory;
import com.business.erp.settings.enums.SettingKey;
import com.business.erp.settings.enums.SettingCategory;
import com.business.erp.settings.dto.response.SettingUpdateResult;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import com.business.erp.settings.dto.response.SettingResponse;

public interface SettingsService {
    String getCurrentValue(SettingKey key);

    int getIntValue(SettingKey key);

    BigDecimal getDecimalValue(SettingKey key);

    boolean getBooleanValue(SettingKey key);

    String getValueAsOf(SettingKey key, LocalDate asOfDate);

    int getIntValueAsOf(SettingKey key, LocalDate asOfDate);

    BigDecimal getDecimalValueAsOf(SettingKey key, LocalDate asOfDate);

    SettingUpdateResult updateSetting(SettingKey key, String newValue, String changedBy);

    List<AppSetting> getAllSettings();

    List<AppSetting> getAllSettings(SettingCategory category);
    List<SettingResponse> getAllSettingResponses(SettingCategory category);

    List<AppSettingHistory> getHistory(SettingKey key);

    void activateDueSettings();
}
