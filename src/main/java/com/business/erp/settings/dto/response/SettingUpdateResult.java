package com.business.erp.settings.dto.response;
import lombok.*;
@Data @Builder public class SettingUpdateResult { private SettingResponse setting; private String updateMode; private boolean replacedExistingPending; private String message; }
