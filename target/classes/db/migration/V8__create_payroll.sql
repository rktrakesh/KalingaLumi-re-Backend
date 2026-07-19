-- V8__create_payroll.sql

CREATE TABLE payroll_runs
(
    id              BIGINT      NOT NULL AUTO_INCREMENT,
    run_reference   VARCHAR(30) NOT NULL,
    year            INT         NOT NULL,
    month           INT         NOT NULL,
    period_start    DATE        NOT NULL,
    period_end      DATE        NOT NULL,
    status          VARCHAR(20) NOT NULL DEFAULT 'GENERATED',
    total_employees INT,
    total_gross     DECIMAL(15, 2),
    total_net       DECIMAL(15, 2),
    generated_by    VARCHAR(50) NOT NULL,
    generated_date  DATETIME    NOT NULL,
    locked_by       VARCHAR(50),
    locked_date     DATETIME,
    remarks         TEXT,
    PRIMARY KEY (id),
    UNIQUE KEY uq_payroll_run_reference (run_reference),
    UNIQUE KEY uq_payroll_run_month (year, month),
    KEY             idx_payroll_run_year_month (year, month)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE payroll_details
(
    id                       BIGINT         NOT NULL AUTO_INCREMENT,
    payroll_run_id           BIGINT         NOT NULL,
    employee_id              BIGINT         NOT NULL,
    employee_code            VARCHAR(20)    NOT NULL,
    employee_name            VARCHAR(100)   NOT NULL,
    base_salary              DECIMAL(12, 2) NOT NULL,
    standard_work_days       INT            NOT NULL,
    standard_work_hours      INT            NOT NULL,
    hourly_rate              DECIMAL(12, 4) NOT NULL,
    worked_minutes           INT            NOT NULL,
    paid_leave_days          INT            NOT NULL DEFAULT 0,
    overtime_minutes         INT            NOT NULL DEFAULT 0,
    overtime_multiplier      DECIMAL(4, 2)  NOT NULL,
    gross_salary             DECIMAL(12, 2) NOT NULL,
    loan_interest_deduction  DECIMAL(12, 2) NOT NULL DEFAULT 0,
    loan_principal_deduction DECIMAL(12, 2) NOT NULL DEFAULT 0,
    total_deductions         DECIMAL(12, 2) NOT NULL DEFAULT 0,
    net_salary               DECIMAL(12, 2) NOT NULL,
    salary_capped            BOOLEAN        NOT NULL DEFAULT FALSE,
    payment_status           VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    paid_date                DATE,
    created_by               VARCHAR(50),
    created_date             DATETIME       NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uq_payroll_detail_run_emp (payroll_run_id, employee_id),
    CONSTRAINT fk_pd_run FOREIGN KEY (payroll_run_id) REFERENCES payroll_runs (id),
    CONSTRAINT fk_pd_employee FOREIGN KEY (employee_id) REFERENCES employees (id),
    KEY                      idx_payroll_detail_emp (employee_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

