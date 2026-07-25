-- V32__leave_carry_forward_and_settlement.sql
-- Supports the UNUSED PAID LEAVE POLICY (EXPIRE / ENCASH / CARRY_FORWARD).

ALTER TABLE leave_balances
    ADD COLUMN carried_forward_in INT NOT NULL DEFAULT 0 COMMENT 'Days brought in from previous month, already included in allocated';

-- One row per employee per month recording how the unused balance was settled at
-- month end (created when payroll for that month is APPROVED). Immutable audit trail.
CREATE TABLE leave_settlement_logs
(
    id                  BIGINT         NOT NULL AUTO_INCREMENT,
    payroll_run_id      BIGINT         NOT NULL,
    employee_id         BIGINT         NOT NULL,
    year                INT            NOT NULL,
    month               INT            NOT NULL,
    unused_days         INT            NOT NULL,
    policy_applied      VARCHAR(30)    NOT NULL,
    expired_days        INT            NOT NULL DEFAULT 0,
    encashed_days        INT           NOT NULL DEFAULT 0,
    encashment_amount    DECIMAL(12, 2) NOT NULL DEFAULT 0,
    carried_forward_days INT           NOT NULL DEFAULT 0,
    carry_forward_limit  INT           NOT NULL DEFAULT 0,
    settled_by          VARCHAR(50)    NOT NULL,
    settled_date         DATETIME      NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uq_leave_settlement_emp_month (employee_id, year, month),
    CONSTRAINT fk_leave_settlement_run FOREIGN KEY (payroll_run_id) REFERENCES payroll_runs (id),
    CONSTRAINT fk_leave_settlement_employee FOREIGN KEY (employee_id) REFERENCES employees (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;