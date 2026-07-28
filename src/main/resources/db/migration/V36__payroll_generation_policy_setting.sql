-- V36__payroll_generation_policy_setting.sql
-- New configurable policy controlling WHEN payroll may be generated for a period.
-- GENERATE_AFTER_PERIOD_END (default) blocks generation until the period has fully
-- ended; ALLOW_DRAFT_GENERATION removes that restriction (existing DRAFT-starting
-- lifecycle behaviour is unchanged either way).

INSERT INTO app_settings (setting_key, setting_value, description, effective_from_date, created_by, created_date)
VALUES ('PAYROLL_GENERATION_POLICY', 'GENERATE_AFTER_PERIOD_END',
        'GENERATE_AFTER_PERIOD_END (recommended) blocks payroll generation until the period has ended; ALLOW_DRAFT_GENERATION removes that restriction',
        '2024-01-01', 'SYSTEM', NOW());

INSERT INTO app_settings_history (setting_key, old_value, new_value, effective_from_date, changed_by, changed_date)
SELECT setting_key, NULL, setting_value, effective_from_date, 'SYSTEM', NOW()
FROM app_settings
WHERE setting_key = 'PAYROLL_GENERATION_POLICY';