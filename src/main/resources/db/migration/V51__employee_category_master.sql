-- V51__employee_category_master.sql
-- Replaces the Java EmployeeCategory enum as the single source of truth. `code` is
-- immutable by convention (enforced in the service layer) — existing business logic in
-- Overtime/Payroll/Performance compares against these exact 4 codes via the
-- EmployeeCategoryCode constants class, so they must never change once seeded.

CREATE TABLE employee_category_master
(
    id           BIGINT       NOT NULL AUTO_INCREMENT,
    code         VARCHAR(50)  NOT NULL,
    name         VARCHAR(100) NOT NULL,
    description  VARCHAR(255),
    active       BOOLEAN      NOT NULL DEFAULT TRUE,
    created_by   VARCHAR(50),
    created_date DATETIME     NOT NULL,
    updated_by   VARCHAR(50),
    updated_date DATETIME,
    PRIMARY KEY (id),
    UNIQUE KEY uq_employee_category_code (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO employee_category_master (code, name, description, active, created_by, created_date)
VALUES ('FACTORY', 'Factory',
        'Production-floor employees — machine operators, supervisors, helpers. Overtime-eligible.', TRUE, 'SYSTEM',
        NOW()),
       ('SALES', 'Sales', 'Sales & Marketing employees — tracked by the Performance Engine instead of Overtime.', TRUE,
        'SYSTEM', NOW()),
       ('ADMINISTRATION', 'Administration', 'Administrative/support staff — accountants, store keepers, etc.', TRUE,
        'SYSTEM', NOW()),
       ('MANAGEMENT', 'Management', 'Leadership and management-level employees.', TRUE, 'SYSTEM', NOW());