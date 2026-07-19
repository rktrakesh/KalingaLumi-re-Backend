package com.business.erp.settings.service.impl;

import com.business.erp.common.exception.BusinessException;
import com.business.erp.settings.entity.AppSetting;
import com.business.erp.settings.entity.AppSettingHistory;
import com.business.erp.settings.enums.SettingKey;
import com.business.erp.settings.repository.AppSettingHistoryRepository;
import com.business.erp.settings.repository.AppSettingRepository;
import com.business.erp.settings.service.SettingsService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SettingsServiceImpl implements SettingsService {

    private final AppSettingRepository settingRepository;
    private final AppSettingHistoryRepository historyRepository;
    private final Logger log = LoggerFactory.getLogger(SettingsServiceImpl.class);

    @Override
    public String getCurrentValue(SettingKey key) {
        return settingRepository.findBySettingKey(key.name())
                .map(AppSetting::getSettingValue)
                .orElseThrow(() -> {
                    log.error("SettingsServiceImpl:getCurrentValue :: Setting not found key={}", key);
                    return new BusinessException("Setting not found: " + key);
                });
    }

    @Override
    public int getIntValue(SettingKey key) {
        return Integer.parseInt(getCurrentValue(key));
    }

    @Override
    public BigDecimal getDecimalValue(SettingKey key) {
        return new BigDecimal(getCurrentValue(key));
    }

    @Override
    public boolean getBooleanValue(SettingKey key) {
        return Boolean.parseBoolean(getCurrentValue(key));
    }

    @Override
    public String getValueAsOf(SettingKey key, LocalDate asOfDate) {
        log.debug("SettingsServiceImpl:getValueAsOf :: key={} asOfDate={}", key, asOfDate);
        return historyRepository.findMostRecentAsOf(key.name(), asOfDate)
                .map(AppSettingHistory::getNewValue)
                .orElseGet(() -> getCurrentValue(key));
    }

    @Override
    public int getIntValueAsOf(SettingKey key, LocalDate asOfDate) {
        return Integer.parseInt(getValueAsOf(key, asOfDate));
    }

    @Override
    public BigDecimal getDecimalValueAsOf(SettingKey key, LocalDate asOfDate) {
        return new BigDecimal(getValueAsOf(key, asOfDate));
    }

    @Override
    @Transactional
    public AppSetting updateSetting(SettingKey key, String newValue, String changedBy) {
        log.info("SettingsServiceImpl:updateSetting :: key={} newValue={} changedBy={}", key, newValue, changedBy);
        AppSetting setting = settingRepository.findBySettingKey(key.name())
                .orElseThrow(() -> new BusinessException("Setting not found: " + key));

        LocalDate nextMonthStart = LocalDate.now().plusMonths(1).withDayOfMonth(1);
        historyRepository.save(AppSettingHistory.builder()
                .settingKey(key.name()).oldValue(setting.getSettingValue()).newValue(newValue)
                .effectiveFromDate(nextMonthStart).changedBy(changedBy).changedDate(LocalDateTime.now())
                .build());

        setting.setSettingValue(newValue);
        setting.setEffectiveFromDate(nextMonthStart);
        AppSetting saved = settingRepository.save(setting);
        log.info("SettingsServiceImpl:updateSetting :: SUCCESS key={} effectiveFrom={}", key, nextMonthStart);
        return saved;
    }

    @Override
    public List<AppSetting> getAllSettings() {
        return settingRepository.findAll();
    }

    @Override
    public List<AppSettingHistory> getHistory(SettingKey key) {
        return historyRepository.findBySettingKeyOrderByChangedDateDesc(key.name());
    }
}
