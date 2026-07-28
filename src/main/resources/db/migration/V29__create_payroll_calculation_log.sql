-- V29__create_payroll_calculation_log.sql
-- Per-employee, per-calculation-version audit trail of every number that fed into a
-- payslip. Never overwritten — a recalculation inserts a new row with a new
-- calculation_version. This is the primary artifact used for payroll dispute resolution.

CREATE TABLE payroll_calculation_logs
(
    id                       BIGINT         NOT NULL AUTO_INCREMENT,
    payroll_run_id           BIGINT         NOT NULL,
    payroll_detail_id        BIGINT         NULL,
    employee_id              BIGINT         NOT NULL,
    calculation_version      INT            NOT NULL,

    monthly_salary           DECIMAL(12, 2) NOT NULL,
    hourly_rate              DECIMAL(12, 4) NOT NULL,

    present_days             INT            NOT NULL DEFAULT 0,
    weekly_off_days          INT            NOT NULL DEFAULT 0,
    weekly_off_worked_days   INT            NOT NULL DEFAULT 0,
    holiday_days             INT            NOT NULL DEFAULT 0,
    holiday_worked_days      INT            NOT NULL DEFAULT 0,
    paid_leave_days          INT            NOT NULL DEFAULT 0,
    automatic_paid_leave_days INT           NOT NULL DEFAULT 0,
    absent_days              INT            NOT NULL DEFAULT 0,

    approved_ot_minutes      INT            NOT NULL DEFAULT 0,
    holiday_ot_minutes       INT            NOT NULL DEFAULT 0,
    weekly_off_ot_minutes    INT            NOT NULL DEFAULT 0,

    basic_salary_amount      DECIMAL(12, 2) NOT NULL DEFAULT 0,
    overtime_amount          DECIMAL(12, 2) NOT NULL DEFAULT 0,
    weekly_off_pay_amount    DECIMAL(12, 2) NOT NULL DEFAULT 0,
    holiday_ot_amount        DECIMAL(12, 2) NOT NULL DEFAULT 0,
    leave_encashment_days    DECIMAL(6, 2)  NOT NULL DEFAULT 0,
    leave_encashment_amount  DECIMAL(12, 2) NOT NULL DEFAULT 0,
    loss_of_pay_amount       DECIMAL(12, 2) NOT NULL DEFAULT 0,

    gross_salary             DECIMAL(12, 2) NOT NULL DEFAULT 0,
    final_net_salary         DECIMAL(12, 2) NOT NULL DEFAULT 0,

    calculated_by            VARCHAR(50)    NOT NULL,
    calculated_date          DATETIME       NOT NULL,

    PRIMARY KEY (id),
    CONSTRAINT fk_pcl_run FOREIGN KEY (payroll_run_id) REFERENCES payroll_runs (id),
    CONSTRAINT fk_pcl_employee FOREIGN KEY (employee_id) REFERENCES employees (id),
    KEY idx_pcl_run_emp (payroll_run_id, employee_id),
    KEY idx_pcl_emp_version (employee_id, calculation_version)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;