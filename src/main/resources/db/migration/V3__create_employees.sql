-- V3__create_employees.sql

CREATE TABLE employees
(
    id             BIGINT         NOT NULL AUTO_INCREMENT,
    employee_code  VARCHAR(20)    NOT NULL,
    name           VARCHAR(100)   NOT NULL,
    phone          VARCHAR(20),
    address        TEXT,
    joining_date   DATE           NOT NULL,
    designation    VARCHAR(100),
    current_salary DECIMAL(12, 2) NOT NULL,
    status         VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_by     VARCHAR(50),
    created_date   DATETIME       NOT NULL,
    updated_by     VARCHAR(50),
    updated_date   DATETIME,
    PRIMARY KEY (id),
    UNIQUE KEY uq_employee_code (employee_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE employee_salary_history
(
    id             BIGINT         NOT NULL AUTO_INCREMENT,
    employee_id    BIGINT         NOT NULL,
    salary         DECIMAL(12, 2) NOT NULL,
    effective_from DATE           NOT NULL,
    remarks        VARCHAR(500),
    created_by     VARCHAR(50)    NOT NULL,
    created_date   DATETIME       NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_salary_hist_employee FOREIGN KEY (employee_id) REFERENCES employees (id),
    KEY            idx_salary_hist_employee (employee_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Now add FK from users -> employees (employees table now exists)
ALTER TABLE users
    ADD CONSTRAINT fk_user_employee FOREIGN KEY (employee_id) REFERENCES employees (id);

-- Sequence table for employee codes
CREATE TABLE seq_employee
(
    next_val BIGINT NOT NULL DEFAULT 1
) ENGINE=InnoDB;
INSERT INTO seq_employee
VALUES (1);
