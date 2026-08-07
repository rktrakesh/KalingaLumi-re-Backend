-- V49__insert_iam_settings.sql
-- Password policy and account-lock threshold — fully configurable from Settings, exactly
-- like every other business rule in this ERP. No code change is ever needed to adjust
-- these; they're read live by PasswordPolicyService/AuthServiceImpl on every check.

INSERT INTO app_settings (setting_key, setting_value, description, effective_from_date, created_by, created_date)
VALUES ('PASSWORD_MIN_LENGTH', '8', 'Minimum password length', '2024-01-01', 'SYSTEM', NOW()),
       ('PASSWORD_REQUIRE_UPPERCASE', 'true', 'Require at least one uppercase letter', '2024-01-01', 'SYSTEM', NOW()),
       ('PASSWORD_REQUIRE_LOWERCASE', 'true', 'Require at least one lowercase letter', '2024-01-01', 'SYSTEM', NOW()),
       ('PASSWORD_REQUIRE_NUMBER', 'true', 'Require at least one digit', '2024-01-01', 'SYSTEM', NOW()),
       ('PASSWORD_REQUIRE_SPECIAL_CHAR', 'true', 'Require at least one special character', '2024-01-01', 'SYSTEM',
        NOW()),
       ('PASSWORD_HISTORY_COUNT', '3', 'Number of previous passwords a user may not reuse', '2024-01-01', 'SYSTEM',
        NOW()),
       ('MAX_FAILED_LOGIN_ATTEMPTS', '5', 'Consecutive failed logins before the account is automatically locked',
        '2024-01-01', 'SYSTEM', NOW()),
       ('PASSWORD_RESET_TOKEN_EXPIRY_MINUTES', '30', 'Minutes a Forgot Password reset link stays valid', '2024-01-01',
        'SYSTEM', NOW());

INSERT INTO app_settings_history (setting_key, old_value, new_value, effective_from_date, changed_by, changed_date)
SELECT setting_key, NULL, setting_value, effective_from_date, 'SYSTEM', NOW()
FROM app_settings
WHERE setting_key IN ('PASSWORD_MIN_LENGTH', 'PASSWORD_REQUIRE_UPPERCASE', 'PASSWORD_REQUIRE_LOWERCASE',
                      'PASSWORD_REQUIRE_NUMBER', 'PASSWORD_REQUIRE_SPECIAL_CHAR', 'PASSWORD_HISTORY_COUNT',
                      'MAX_FAILED_LOGIN_ATTEMPTS', 'PASSWORD_RESET_TOKEN_EXPIRY_MINUTES');