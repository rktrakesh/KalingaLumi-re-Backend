-- Phase 2 System Settings Engine. V1-V55 already provide app_settings,
-- app_settings_history, employees, and seq_employee.
-- app_settings contains active values only; app_setting_schedules is the sole
-- pending-state lifecycle table.

ALTER TABLE app_settings
    ADD COLUMN data_type VARCHAR(20) NOT NULL DEFAULT 'STRING' AFTER setting_category,
    ADD COLUMN editable BOOLEAN NOT NULL DEFAULT TRUE AFTER data_type;

ALTER TABLE app_settings_history
    ADD COLUMN change_type VARCHAR(30) NOT NULL DEFAULT 'LEGACY' AFTER setting_key;

-- Existing records are audit evidence only; they cannot prove a historical active value.
UPDATE app_settings_history SET change_type = 'LEGACY';

CREATE TABLE app_setting_schedules (
    id BIGINT NOT NULL AUTO_INCREMENT,
    setting_key VARCHAR(100) NOT NULL,
    scheduled_value VARCHAR(500) NOT NULL,
    effective_date DATE NOT NULL,
    status VARCHAR(20) NOT NULL,
    failure_reason VARCHAR(500) NULL,
    created_by VARCHAR(50) NOT NULL,
    created_date DATETIME NOT NULL,
    updated_date DATETIME NULL,
    pending_setting_key VARCHAR(100)
        GENERATED ALWAYS AS (CASE WHEN status = 'PENDING' THEN setting_key ELSE NULL END) STORED,
    pending_effective_date DATE
        GENERATED ALWAYS AS (CASE WHEN status = 'PENDING' THEN effective_date ELSE NULL END) STORED,
    PRIMARY KEY (id),
    UNIQUE KEY uq_setting_schedule_live_pending (pending_setting_key, pending_effective_date),
    KEY idx_setting_schedule_due (status, effective_date),
    CONSTRAINT chk_setting_schedule_status
        CHECK (status IN ('PENDING', 'ACTIVATED', 'REPLACED', 'CANCELLED', 'FAILED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

ALTER TABLE app_settings_history
    ADD CONSTRAINT chk_settings_history_event_type
        CHECK (change_type IN ('LEGACY', 'BASELINE', 'IMMEDIATE_UPDATE', 'SCHEDULED',
                               'REPLACED', 'CANCELLED', 'ACTIVATED',
                               'ACTIVATION_FAILED', 'RETRY_REQUESTED'));

CREATE INDEX idx_settings_history_asof
    ON app_settings_history (setting_key, change_type, effective_from_date);

-- Insert only missing Phase 2 settings. Generic defaults avoid company-specific branding.
INSERT INTO app_settings (setting_key, setting_category, data_type, editable, setting_value,
                          description, effective_from_date, created_by, created_date)
SELECT defaults.setting_key, defaults.setting_category, defaults.data_type, defaults.editable,
       defaults.setting_value, defaults.description, CURDATE(), 'SYSTEM', NOW()
FROM (
    SELECT 'COMPANY_NAME' setting_key, 'ORGANIZATION' setting_category, 'STRING' data_type, TRUE editable,
           'ERP System' setting_value, 'Company legal name' description
    UNION ALL SELECT 'COMPANY_SHORT_NAME', 'ORGANIZATION', 'STRING', TRUE, 'ERP', 'Short company name used in identifiers'
    UNION ALL SELECT 'COMPANY_GST_NUMBER', 'ORGANIZATION', 'STRING', TRUE, '', 'Company GST number'
    UNION ALL SELECT 'COMPANY_PAN_NUMBER', 'ORGANIZATION', 'STRING', TRUE, '', 'Company PAN number'
    UNION ALL SELECT 'COMPANY_EMAIL', 'ORGANIZATION', 'EMAIL', TRUE, '', 'Company contact email'
    UNION ALL SELECT 'COMPANY_PHONE', 'ORGANIZATION', 'STRING', TRUE, '', 'Company contact phone'
    UNION ALL SELECT 'COMPANY_WEBSITE', 'ORGANIZATION', 'URL', TRUE, '', 'Company website URL'
    UNION ALL SELECT 'COMPANY_ADDRESS', 'ORGANIZATION', 'STRING', TRUE, '', 'Company address'
    UNION ALL SELECT 'COMPANY_LOGO_URL', 'ORGANIZATION', 'URL', TRUE, '', 'Public company logo URL'
    UNION ALL SELECT 'EMPLOYEE_CODE_TEMPLATE', 'EMPLOYEE_HR', 'STRING', TRUE,
                     '{CompanyShortName}{FirstName4}{EmployeeNumber}', 'Template for new employee codes'
    UNION ALL SELECT 'USERNAME_GENERATION_RULE', 'EMPLOYEE_HR', 'STRING', TRUE,
                     '{CompanyShortName}*{FirstName}*{EmployeeNumber}', 'Template for new usernames'
    UNION ALL SELECT 'EMPLOYEE_CODE_PREFIX', 'EMPLOYEE_HR', 'STRING', FALSE, '',
                     'Legacy employee-code prefix metadata; not used for new employee generation'
) defaults
WHERE NOT EXISTS (SELECT 1 FROM app_settings s WHERE s.setting_key = defaults.setting_key);

-- Metadata drives typed validation and frontend controls without changing values.
UPDATE app_settings
SET data_type = CASE setting_key
    WHEN 'PAID_LEAVES_PER_MONTH' THEN 'INTEGER'
    WHEN 'STANDARD_WORKING_HOURS' THEN 'INTEGER'
    WHEN 'STANDARD_WORKING_DAYS' THEN 'INTEGER'
    WHEN 'SALARY_PAYMENT_DAY' THEN 'INTEGER'
    WHEN 'PASSWORD_MIN_LENGTH' THEN 'INTEGER'
    WHEN 'PASSWORD_HISTORY_COUNT' THEN 'INTEGER'
    WHEN 'MAX_FAILED_LOGIN_ATTEMPTS' THEN 'INTEGER'
    WHEN 'PASSWORD_RESET_TOKEN_EXPIRY_MINUTES' THEN 'INTEGER'
    WHEN 'LEAVE_CARRY_FORWARD_LIMIT' THEN 'INTEGER'
    WHEN 'OVERTIME_MULTIPLIER' THEN 'DECIMAL'
    WHEN 'LOAN_DEFAULT_INTEREST_RATE' THEN 'DECIMAL'
    WHEN 'WEEKLY_OFF_MULTIPLIER' THEN 'DECIMAL'
    WHEN 'HOLIDAY_OT_MULTIPLIER' THEN 'DECIMAL'
    WHEN 'LOW_STOCK_ALERT_ENABLED' THEN 'BOOLEAN'
    WHEN 'MONTH_CLOSING_REQUIRES_PAYROLL' THEN 'BOOLEAN'
    WHEN 'LEAVE_ENCASHMENT_ENABLED' THEN 'BOOLEAN'
    WHEN 'PASSWORD_REQUIRE_UPPERCASE' THEN 'BOOLEAN'
    WHEN 'PASSWORD_REQUIRE_LOWERCASE' THEN 'BOOLEAN'
    WHEN 'PASSWORD_REQUIRE_NUMBER' THEN 'BOOLEAN'
    WHEN 'PASSWORD_REQUIRE_SPECIAL_CHAR' THEN 'BOOLEAN'
    WHEN 'COMPANY_EMAIL' THEN 'EMAIL'
    WHEN 'COMPANY_WEBSITE' THEN 'URL'
    WHEN 'COMPANY_LOGO_URL' THEN 'URL'
    ELSE data_type
END;

UPDATE app_settings SET editable = FALSE WHERE setting_key = 'EMPLOYEE_CODE_PREFIX';

-- Strict historical cutover: baseline values are the active settings at deployment.
INSERT INTO app_settings_history (setting_key, change_type, old_value, new_value,
                                  effective_from_date, changed_by, changed_date)
SELECT setting_key, 'BASELINE', NULL, setting_value, CURDATE(), 'SYSTEM', NOW()
FROM app_settings;
