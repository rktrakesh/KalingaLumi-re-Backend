package com.business.erp.auth.enums;

public enum LoginAuditEventType {
    LOGIN_SUCCESS,
    LOGIN_FAILURE,
    LOGOUT,
    PASSWORD_CHANGE,
    FORGOT_PASSWORD_REQUESTED,
    PASSWORD_RESET,
    ACCOUNT_LOCKED,
    ACCOUNT_UNLOCKED
}