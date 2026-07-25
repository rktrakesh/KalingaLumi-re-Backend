-- V28__create_payroll_settings_snapshot.sql
-- Immutable capture of every payroll-relevant setting at the moment payroll for a
-- month is FIRST generated. Historical payroll always recalculates against its own
-- snapshot, even if company settings change afterwards.

CREATE TABLE payroll_settings_snapshots
(
    id                        BIGINT         NOT NULL AUTO_INCREMENT,
    payroll_run_id            BIGINT         NOT NULL,
    standard_working_days     INT            NOT NULL,
    working_hours_per_day     INT            NOT NULL,
    paid_leaves_per_month     INT            NOT NULL,
    overtime_multiplier       DECIMAL(6, 3)  NOT NULL,
    weekly_off_multiplier     DECIMAL(6, 3)  NOT NULL,
    holiday_ot_multiplier     DECIMAL(6, 3)  NOT NULL,
    weekly_off_days           VARCHAR(20)    NOT NULL,
    leave_allocation_method   VARCHAR(30)    NOT NULL,
    unused_leave_policy       VARCHAR(30)    NOT NULL,
    leave_carry_forward_limit INT            NOT NULL,
    leave_encashment_enabled  BOOLEAN        NOT NULL,
    captured_by               VARCHAR(50)    NOT NULL,
    captured_date             DATETIME       NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uq_snapshot_run (payroll_run_id),
    CONSTRAINT fk_snapshot_run FOREIGN KEY (payroll_run_id) REFERENCES payroll_runs (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;