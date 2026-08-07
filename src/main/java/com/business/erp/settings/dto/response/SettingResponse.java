package com.business.erp.settings.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Data
@Builder
public class SettingResponse {
    private Long id;
    private String settingKey;
    private String settingCategory;
    private String dataType;
    private boolean editable;
    private String activeValue;
    private LocalDate activeEffectiveFromDate;
    private String settingValue;
    private String pendingValue;
    private String description;
    private LocalDate effectiveFromDate;
    private LocalDate pendingEffectiveDate;
    private String pendingStatus;
    private String updateMode;
}
