-- V19__insert_default_settings.sql

INSERT INTO app_settings (setting_key, setting_value, description, effective_from_date, created_by, created_date)
VALUES ('PAID_LEAVES_PER_MONTH', '4', 'Monthly paid leave entitlement per employee', '2024-01-01', 'SYSTEM', NOW()),
       ('STANDARD_WORKING_HOURS', '8', 'Standard daily working hours', '2024-01-01', 'SYSTEM', NOW()),
       ('STANDARD_WORKING_DAYS', '26', 'Standard working days per month for salary calculation', '2024-01-01', 'SYSTEM',
        NOW()),
       ('OVERTIME_MULTIPLIER', '1.5', 'Overtime pay multiplier (e.g. 1.5 = time-and-a-half)', '2024-01-01', 'SYSTEM',
        NOW()),
       ('LOAN_DEFAULT_INTEREST_RATE', '2.0', 'Default monthly loan interest percentage', '2024-01-01', 'SYSTEM', NOW()),
       ('SALARY_PAYMENT_DAY', '5', 'Day of following month when salary is paid', '2024-01-01', 'SYSTEM', NOW()),
       ('LOW_STOCK_ALERT_ENABLED', 'true', 'Enable low inventory notifications', '2024-01-01', 'SYSTEM', NOW()),
       ('MONTH_CLOSING_REQUIRES_PAYROLL', 'true', 'Block month closing if payroll not yet generated', '2024-01-01',
        'SYSTEM', NOW());

-- Seed initial settings history
INSERT INTO app_settings_history (setting_key, old_value, new_value, effective_from_date, changed_by, changed_date)
SELECT setting_key, NULL, setting_value, effective_from_date, 'SYSTEM', NOW()
FROM app_settings;
