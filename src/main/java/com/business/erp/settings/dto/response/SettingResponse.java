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
    private String settingValue;
    private String description;
    private LocalDate effectiveFromDate;
}
