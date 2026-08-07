package com.business.erp.settings.service.impl;

import com.business.erp.common.exception.BusinessException;
import com.business.erp.common.clock.ClockProvider;
import java.time.Clock;
import com.business.erp.settings.entity.AppSetting;
import com.business.erp.settings.entity.AppSettingHistory;
import com.business.erp.settings.enums.SettingKey;
import com.business.erp.settings.enums.SettingCategory;
import com.business.erp.settings.enums.SettingHistoryEventType;
import com.business.erp.settings.repository.AppSettingHistoryRepository;
import com.business.erp.settings.repository.AppSettingRepository;
import com.business.erp.settings.service.SettingsService;
import com.business.erp.settings.service.IdentifierTemplateRenderer;
import com.business.erp.settings.service.SettingsActivationService;
import com.business.erp.settings.dto.response.*;
import com.business.erp.settings.entity.AppSettingSchedule;
import com.business.erp.settings.enums.SettingScheduleStatus;
import com.business.erp.settings.repository.AppSettingScheduleRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class SettingsServiceImpl implements SettingsService {

    private final AppSettingRepository settingRepository;
    private final AppSettingHistoryRepository historyRepository;
    private final AppSettingScheduleRepository scheduleRepository;
    private final SettingsActivationService activationService;
    private final ClockProvider clockProvider;
    private final Clock clock;
    private final Logger log = LoggerFactory.getLogger(SettingsServiceImpl.class);
    private static final Set<SettingKey> IMMEDIATE = Set.of(SettingKey.COMPANY_NAME, SettingKey.COMPANY_SHORT_NAME,
            SettingKey.COMPANY_GST_NUMBER, SettingKey.COMPANY_PAN_NUMBER, SettingKey.COMPANY_EMAIL, SettingKey.COMPANY_PHONE,
            SettingKey.COMPANY_WEBSITE, SettingKey.COMPANY_ADDRESS, SettingKey.COMPANY_LOGO_URL,
            SettingKey.EMPLOYEE_CODE_TEMPLATE, SettingKey.USERNAME_GENERATION_RULE);

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
        LocalDate cutover = historyRepository.findCutoverDate(key.name()).orElseThrow(() -> new BusinessException("Historical setting value is unavailable before settings-history cutover date: no baseline for " + key));
        if (asOfDate.isBefore(cutover)) throw new BusinessException("Historical setting value is unavailable before settings-history cutover date: " + cutover);
        return historyRepository.findMostRecentAsOf(key.name(), asOfDate)
                .map(AppSettingHistory::getNewValue)
                .orElseThrow(() -> new BusinessException("No historical value found for " + key + " on " + asOfDate));
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
    public SettingUpdateResult updateSetting(SettingKey key, String newValue, String changedBy) {
        log.info("SettingsServiceImpl:updateSetting :: key={} newValue={} changedBy={}", key, newValue, changedBy);
        AppSetting setting = settingRepository.findBySettingKey(key.name())
                .orElseThrow(() -> new BusinessException("Setting not found: " + key));

        validate(setting, key, newValue);
        LocalDate effective = IMMEDIATE.contains(key) ? clockProvider.today() : clockProvider.today().plusMonths(1).withDayOfMonth(1);
        boolean replaced = false;
        if (!setting.isEditable()) throw new BusinessException("Setting is system-managed and cannot be edited: " + key);
        if (IMMEDIATE.contains(key)) {
            historyRepository.save(history(key, setting.getSettingValue(), newValue, effective, changedBy, SettingHistoryEventType.IMMEDIATE_UPDATE));
            setting.setSettingValue(newValue);
            setting.setEffectiveFromDate(effective);
        } else {
            List<AppSettingSchedule> existing = scheduleRepository.lockPendingBySettingKeyAndEffectiveDate(key.name(), effective, SettingScheduleStatus.PENDING);
            for (AppSettingSchedule old : existing) { old.setStatus(SettingScheduleStatus.REPLACED); old.setUpdatedDate(LocalDateTime.now(clock)); historyRepository.save(history(key, setting.getSettingValue(), old.getScheduledValue(), effective, changedBy, SettingHistoryEventType.REPLACED)); replaced = true; }
            scheduleRepository.saveAllAndFlush(existing);
            scheduleRepository.save(AppSettingSchedule.builder().settingKey(key.name()).scheduledValue(newValue).effectiveDate(effective).status(SettingScheduleStatus.PENDING).createdBy(changedBy).createdDate(LocalDateTime.now(clock)).build());
            historyRepository.save(history(key, setting.getSettingValue(), newValue, effective, changedBy, SettingHistoryEventType.SCHEDULED));
        }
        AppSetting saved = settingRepository.save(setting);
        log.info("SettingsServiceImpl:updateSetting :: SUCCESS key={} effectiveFrom={}", key, effective);
        SettingResponse response = toResponse(saved);
        return SettingUpdateResult.builder().setting(response).updateMode(response.getUpdateMode()).replacedExistingPending(replaced).message(IMMEDIATE.contains(key) ? "Setting updated immediately." : "Setting scheduled for activation.").build();
    }

    @Override
    public List<AppSetting> getAllSettings() {
        return settingRepository.findAll();
    }

    @Override
    public List<AppSetting> getAllSettings(SettingCategory category) {
        return category == null ? getAllSettings() : settingRepository.findBySettingCategory(category);
    }

    @Override
    public List<SettingResponse> getAllSettingResponses(SettingCategory category) {
        return getAllSettings(category).stream().map(this::toResponse).toList();
    }

    @Override
    public List<AppSettingHistory> getHistory(SettingKey key) {
        return historyRepository.findBySettingKeyOrderByChangedDateDesc(key.name());
    }

    @Override
    public void activateDueSettings() {
        activationService.activateDueSchedules();
    }

    private AppSettingHistory history(SettingKey key, String oldValue, String newValue, LocalDate effective, String by, SettingHistoryEventType type) {
        return AppSettingHistory.builder().settingKey(key.name()).changeType(type).oldValue(oldValue).newValue(newValue)
                .effectiveFromDate(effective).changedBy(by).changedDate(LocalDateTime.now(clock)).build();
    }

    private void validate(AppSetting setting, SettingKey key, String value) {
        if (value == null || value.isBlank() && key == SettingKey.COMPANY_NAME) throw new BusinessException(key + " is required");
        if (key == SettingKey.EMPLOYEE_CODE_TEMPLATE || key == SettingKey.USERNAME_GENERATION_RULE)
            IdentifierTemplateRenderer.validateTemplate(value);
        String dataType = setting.getDataType() == null ? "STRING" : setting.getDataType();
        try {
            switch (dataType) {
                case "INTEGER" -> Integer.parseInt(value);
                case "DECIMAL" -> new BigDecimal(value);
                case "BOOLEAN" -> {
                    if (!"true".equalsIgnoreCase(value) && !"false".equalsIgnoreCase(value)) {
                        throw new IllegalArgumentException();
                    }
                }
                default -> { }
            }
        } catch (IllegalArgumentException exception) {
            throw new BusinessException("Invalid " + dataType.toLowerCase() + " value for " + key);
        }
        if (key == SettingKey.COMPANY_EMAIL && !value.isBlank() && !value.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) throw new BusinessException("Invalid company email");
        if (key == SettingKey.COMPANY_WEBSITE && !value.isBlank() && !value.matches("https?://.+")) throw new BusinessException("Website must be an HTTP(S) URL");
    }

    private SettingResponse toResponse(AppSetting setting) {
        AppSettingSchedule pending = scheduleRepository.findFirstBySettingKeyAndStatusOrderByEffectiveDateAsc(setting.getSettingKey(), SettingScheduleStatus.PENDING).orElse(null);
        return SettingResponse.builder().id(setting.getId()).settingKey(setting.getSettingKey()).settingCategory(setting.getSettingCategory().name())
                .dataType(setting.getDataType()).editable(setting.isEditable()).description(setting.getDescription())
                .activeValue(setting.getSettingValue()).activeEffectiveFromDate(setting.getEffectiveFromDate()).settingValue(setting.getSettingValue()).effectiveFromDate(setting.getEffectiveFromDate())
                .pendingValue(pending == null ? null : pending.getScheduledValue()).pendingEffectiveDate(pending == null ? null : pending.getEffectiveDate())
                .pendingStatus(pending == null ? null : pending.getStatus().name())
                .updateMode(IMMEDIATE.contains(SettingKey.valueOf(setting.getSettingKey())) ? "IMMEDIATE" : "SCHEDULED").build();
    }
}
