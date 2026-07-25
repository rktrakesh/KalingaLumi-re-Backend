-- V27__insert_payroll_engine_settings.sql
-- New configuration-driven payroll settings. No business rule is hardcoded — every
-- value consumed by the payroll engine comes from this table (via a snapshot at
-- generation time, see V29).

INSERT INTO app_settings (setting_key, setting_value, description, effective_from_date, created_by, created_date)
VALUES ('WEEKLY_OFF_DAYS', '7', 'Comma-separated ISO day-of-week numbers (1=Mon..7=Sun) treated as weekly off', '2024-01-01', 'SYSTEM', NOW()),
       ('WEEKLY_OFF_MULTIPLIER', '1.5', 'Hourly rate multiplier when an employee works on a weekly-off day', '2024-01-01', 'SYSTEM', NOW()),
       ('HOLIDAY_OT_MULTIPLIER', '2.0', 'Hourly rate multiplier when an employee works on a work-allowed holiday', '2024-01-01', 'SYSTEM', NOW()),
       ('LEAVE_ALLOCATION_METHOD', 'MONTHLY_FIXED', 'MONTHLY_FIXED or PRORATED leave allocation for the payroll period', '2024-01-01', 'SYSTEM', NOW()),
       ('UNUSED_PAID_LEAVE_POLICY', 'CARRY_FORWARD', 'EXPIRE, ENCASH or CARRY_FORWARD for unused paid leave at month end', '2024-01-01', 'SYSTEM', NOW()),
       ('LEAVE_CARRY_FORWARD_LIMIT', '5', 'Maximum paid-leave days that may carry forward to next month', '2024-01-01', 'SYSTEM', NOW()),
       ('LEAVE_ENCASHMENT_ENABLED', 'true', 'Master switch for leave encashment (used in addition to the policy value)', '2024-01-01', 'SYSTEM', NOW());

INSERT INTO app_settings_history (setting_key, old_value, new_value, effective_from_date, changed_by, changed_date)
SELECT setting_key, NULL, setting_value, effective_from_date, 'SYSTEM', NOW()
FROM app_settings
WHERE setting_key IN ('WEEKLY_OFF_DAYS', 'WEEKLY_OFF_MULTIPLIER', 'HOLIDAY_OT_MULTIPLIER', 'LEAVE_ALLOCATION_METHOD',
                      'UNUSED_PAID_LEAVE_POLICY', 'LEAVE_CARRY_FORWARD_LIMIT', 'LEAVE_ENCASHMENT_ENABLED');