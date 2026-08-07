-- Settings remain centralized in app_settings. Category is organizational metadata
-- only: it does not rename a key or alter any engine's settings lookup behaviour.

ALTER TABLE app_settings
    ADD COLUMN setting_category VARCHAR(40) NOT NULL DEFAULT 'SYSTEM' AFTER setting_key;

UPDATE app_settings
SET setting_category = CASE setting_key
                           WHEN 'STANDARD_WORKING_HOURS' THEN 'ATTENDANCE'

                           WHEN 'PAID_LEAVES_PER_MONTH' THEN 'LEAVE'
                           WHEN 'LEAVE_ALLOCATION_METHOD' THEN 'LEAVE'
                           WHEN 'UNUSED_PAID_LEAVE_POLICY' THEN 'LEAVE'
                           WHEN 'LEAVE_CARRY_FORWARD_LIMIT' THEN 'LEAVE'
                           WHEN 'LEAVE_ENCASHMENT_ENABLED' THEN 'LEAVE'

                           WHEN 'STANDARD_WORKING_DAYS' THEN 'PAYROLL'
                           WHEN 'OVERTIME_MULTIPLIER' THEN 'PAYROLL'
                           WHEN 'WEEKLY_OFF_DAYS' THEN 'PAYROLL'
                           WHEN 'WEEKLY_OFF_MULTIPLIER' THEN 'PAYROLL'
                           WHEN 'HOLIDAY_OT_MULTIPLIER' THEN 'PAYROLL'
                           WHEN 'SALARY_PAYMENT_DAY' THEN 'PAYROLL'
                           WHEN 'PAYROLL_GENERATION_POLICY' THEN 'PAYROLL'

                           WHEN 'LOAN_DEFAULT_INTEREST_RATE' THEN 'EMPLOYEE_HR'
                           WHEN 'LOW_STOCK_ALERT_ENABLED' THEN 'INVENTORY'
                           WHEN 'MONTH_CLOSING_REQUIRES_PAYROLL' THEN 'FINANCE'

                           WHEN 'PASSWORD_MIN_LENGTH' THEN 'SECURITY_IAM'
                           WHEN 'PASSWORD_REQUIRE_UPPERCASE' THEN 'SECURITY_IAM'
                           WHEN 'PASSWORD_REQUIRE_LOWERCASE' THEN 'SECURITY_IAM'
                           WHEN 'PASSWORD_REQUIRE_NUMBER' THEN 'SECURITY_IAM'
                           WHEN 'PASSWORD_REQUIRE_SPECIAL_CHAR' THEN 'SECURITY_IAM'
                           WHEN 'PASSWORD_HISTORY_COUNT' THEN 'SECURITY_IAM'
                           WHEN 'MAX_FAILED_LOGIN_ATTEMPTS' THEN 'SECURITY_IAM'
                           WHEN 'PASSWORD_RESET_TOKEN_EXPIRY_MINUTES' THEN 'SECURITY_IAM'

                           ELSE 'SYSTEM'
    END;

CREATE INDEX idx_app_settings_category ON app_settings (setting_category);
