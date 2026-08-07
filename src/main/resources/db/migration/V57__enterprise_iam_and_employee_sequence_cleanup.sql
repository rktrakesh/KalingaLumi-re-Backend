-- Phase 3 enterprise IAM and development-schema convergence.
--
-- An earlier development-only V57 introduced employees.joining_sequence. The approved
-- design never persists that value: seq_employee is reserved once and passed directly
-- to employee-code and username rendering. Some development databases recorded that
-- older V57, so enterprise IAM intentionally starts at V58 and removes the obsolete
-- column when present. Fresh databases safely take the no-op branch.

-- V1-V55 are immutable and V56 is the Phase 2 settings foundation. Everything below
-- preserves every user, employee link, token, password-reset record, password-history
-- record, and login-audit record.

-- Fail before making schema changes when legacy identifiers cannot be resolved safely.
-- The temporary table name is intentionally diagnostic in Flyway's migration error.
CREATE
TEMPORARY TABLE phase3_iam_identifier_conflicts_must_be_resolved (
    single_marker TINYINT NOT NULL,
    conflict_type VARCHAR(40) NOT NULL,
    normalized_identifier VARCHAR(50) NOT NULL,
    PRIMARY KEY (single_marker)
);

INSERT INTO phase3_iam_identifier_conflicts_must_be_resolved
    (single_marker, conflict_type, normalized_identifier)
VALUES (1, 'NO_CONFLICT', '<none>');

INSERT INTO phase3_iam_identifier_conflicts_must_be_resolved
    (single_marker, conflict_type, normalized_identifier)
SELECT 1, conflicts.conflict_type, conflicts.normalized_identifier
FROM (SELECT 'BLANK_USERNAME' conflict_type, '<blank>' normalized_identifier
      FROM users
      WHERE TRIM(username) = ''
      UNION ALL
      SELECT 'BLANK_EMPLOYEE_CODE', '<blank>'
      FROM employees
      WHERE TRIM(employee_code) = ''
      UNION ALL
      SELECT 'DUPLICATE_USERNAME', LOWER(TRIM(username))
      FROM users
      GROUP BY LOWER(TRIM(username))
      HAVING COUNT(*) > 1
      UNION ALL
      SELECT 'DUPLICATE_EMPLOYEE_CODE', UPPER(TRIM(employee_code))
      FROM employees
      GROUP BY UPPER(TRIM(employee_code))
      HAVING COUNT(*) > 1
      UNION ALL
      SELECT 'CROSS_IDENTIFIER_COLLISION', LOWER(TRIM(u.username))
      FROM users u
               JOIN employees e
                    ON LOWER(TRIM(u.username)) = LOWER(TRIM(e.employee_code))
      UNION ALL
      SELECT 'UNSUPPORTED_COMPATIBILITY_ROLE', role
      FROM users
      WHERE role NOT IN ('ROLE_ADMIN', 'ROLE_MANAGER', 'ROLE_SUPERVISOR', 'ROLE_HR',
                         'ROLE_FINANCE', 'ROLE_SALES', 'ROLE_EMPLOYEE')) conflicts LIMIT 1;

DROP
TEMPORARY TABLE phase3_iam_identifier_conflicts_must_be_resolved;

-- All fail-fast validation has passed. Remove the development-only column now, before
-- applying the schema that consumes the shared sequence directly in application code.
SET
@drop_joining_sequence_sql = (
    SELECT IF(
        COUNT(*) > 0,
        'ALTER TABLE employees DROP COLUMN joining_sequence',
        'SELECT 1'
    )
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'employees'
      AND column_name = 'joining_sequence'
);
PREPARE drop_joining_sequence_statement FROM @drop_joining_sequence_sql;
EXECUTE drop_joining_sequence_statement;
DEALLOCATE PREPARE drop_joining_sequence_statement;

-- Generated canonical keys enforce case-insensitive, trim-insensitive uniqueness for
-- each identifier namespace without rewriting existing identifiers.
ALTER TABLE users
    MODIFY COLUMN role VARCHAR (50) NOT NULL,
    ADD COLUMN normalized_username VARCHAR (50)
    GENERATED ALWAYS AS (LOWER(username)) STORED,
    ADD CONSTRAINT uq_users_normalized_username UNIQUE (normalized_username),
    ADD COLUMN temporary_password_issued_at DATETIME NULL,
    ADD COLUMN temporary_password_expires_at DATETIME NULL,
    ADD COLUMN last_login_at DATETIME NULL,
    ADD COLUMN credentials_expired BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN token_version BIGINT NOT NULL DEFAULT 0,
    ADD CONSTRAINT chk_users_compatibility_role
    CHECK (role IN ('ROLE_ADMIN', 'ROLE_MANAGER', 'ROLE_SUPERVISOR', 'ROLE_HR',
    'ROLE_FINANCE', 'ROLE_SALES', 'ROLE_EMPLOYEE'));

ALTER TABLE employees
    ADD COLUMN normalized_employee_code VARCHAR(20)
        GENERATED ALWAYS AS (UPPER(employee_code)) STORED,
    ADD CONSTRAINT uq_employees_normalized_employee_code UNIQUE (normalized_employee_code);

UPDATE users
SET credentials_expired = FALSE
WHERE credentials_expired IS NULL;

UPDATE users
SET token_version = 0
WHERE token_version IS NULL;

-- user_roles is authoritative. users.role remains a non-null deterministic
-- compatibility role for existing clients and legacy queries.
CREATE TABLE user_roles
(
    user_id BIGINT      NOT NULL,
    role    VARCHAR(50) NOT NULL,
    PRIMARY KEY (user_id, role),
    KEY     idx_user_roles_role (role, user_id),
    CONSTRAINT fk_user_roles_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT chk_user_roles_supported
        CHECK (role IN ('ROLE_ADMIN', 'ROLE_MANAGER', 'ROLE_SUPERVISOR', 'ROLE_HR',
                        'ROLE_FINANCE', 'ROLE_SALES', 'ROLE_EMPLOYEE'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO user_roles (user_id, role)
SELECT id, role
FROM users;

-- Extend the existing IAM audit table; no second audit store is introduced.
ALTER TABLE login_audit_logs
    ADD COLUMN actor_username VARCHAR(50) NULL AFTER username,
    ADD COLUMN user_agent VARCHAR(500) NULL AFTER ip_address;

-- New policy settings are inserted only when absent. Existing administrator values
-- and metadata are never overwritten.
INSERT INTO app_settings (setting_key, setting_category, data_type, editable, setting_value,
                          description, effective_from_date, created_by, created_date)
SELECT 'TEMPORARY_PASSWORD_EXPIRY_HOURS',
       'SECURITY_IAM',
       'INTEGER',
       TRUE,
       '72',
       'Hours before an administrator-issued temporary password expires',
       CURDATE(),
       'SYSTEM',
       NOW() WHERE NOT EXISTS (
    SELECT 1
    FROM app_settings
    WHERE setting_key = 'TEMPORARY_PASSWORD_EXPIRY_HOURS'
);

-- Preserve the strict Phase 2 cutover strategy for the new setting as well.
INSERT INTO app_settings_history (setting_key, change_type, old_value, new_value,
                                  effective_from_date, changed_by, changed_date)
SELECT setting_key,
       'BASELINE',
       NULL,
       setting_value,
       effective_from_date,
       'SYSTEM',
       NOW()
FROM app_settings setting_row
WHERE setting_row.setting_key = 'TEMPORARY_PASSWORD_EXPIRY_HOURS'
  AND NOT EXISTS (SELECT 1
                  FROM app_settings_history history
                  WHERE history.setting_key = setting_row.setting_key
                    AND history.change_type IN ('BASELINE', 'IMMEDIATE_UPDATE', 'ACTIVATED'));

-- V56 originally used underscores in the seeded username template. The approved stored
-- default uses '*' as a separator placeholder and the renderer normalizes it to '_'.
-- Correct only the untouched SYSTEM seed; never overwrite an administrator-customized
-- value. Record the correction as a value-bearing event before changing the active row.
INSERT INTO app_settings_history (setting_key, change_type, old_value, new_value,
                                  effective_from_date, changed_by, changed_date)
SELECT setting_key,
       'IMMEDIATE_UPDATE',
       setting_value,
       '{CompanyShortName}*{FirstName}*{EmployeeNumber}',
       CURDATE(),
       'SYSTEM',
       NOW()
FROM app_settings
WHERE setting_key = 'USERNAME_GENERATION_RULE'
  AND setting_value = '{CompanyShortName}_{FirstName}_{EmployeeNumber}'
  AND created_by = 'SYSTEM'
  AND updated_by IS NULL;

UPDATE app_settings
SET setting_value       = '{CompanyShortName}*{FirstName}*{EmployeeNumber}',
    effective_from_date = CURDATE(),
    updated_by          = 'SYSTEM',
    updated_date        = NOW()
WHERE setting_key = 'USERNAME_GENERATION_RULE'
  AND setting_value = '{CompanyShortName}_{FirstName}_{EmployeeNumber}'
  AND created_by = 'SYSTEM'
  AND updated_by IS NULL;
