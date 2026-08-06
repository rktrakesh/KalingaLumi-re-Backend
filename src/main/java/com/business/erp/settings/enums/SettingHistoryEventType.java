package com.business.erp.settings.enums;
/** Append-only settings-history event taxonomy. */
public enum SettingHistoryEventType {
    BASELINE, IMMEDIATE_UPDATE, SCHEDULED, REPLACED, CANCELLED, ACTIVATED,
    ACTIVATION_FAILED, RETRY_REQUESTED, LEGACY
}
