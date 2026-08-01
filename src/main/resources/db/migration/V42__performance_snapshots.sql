-- V42__performance_snapshots.sql
-- The Performance Engine's equivalent of payroll_settings_snapshot: once APPROVED, a row
-- here is the permanent historical record. Payroll and reports read ONLY from this table
-- — never from live Orders/Sales Policy — so a later target or slab change can never
-- retroactively change what an employee was actually paid.

CREATE TABLE performance_snapshots
(
    id                       BIGINT         NOT NULL AUTO_INCREMENT,
    employee_id              BIGINT         NOT NULL,
    payroll_run_id           BIGINT         NULL,
    period_year              INT            NOT NULL,
    period_month             INT            NOT NULL,
    sales_policy_id          BIGINT         NULL,
    sales_policy_version     INT            NULL,
    monthly_target           DECIMAL(12, 2) NOT NULL,
    actual_sales             DECIMAL(12, 2) NOT NULL,
    achievement_pct          DECIMAL(6, 2)  NOT NULL,
    incentive_slab_id        BIGINT         NULL,
    incentive_pct_applied    DECIMAL(5, 2)  NOT NULL DEFAULT 0,
    incentive_amount         DECIMAL(12, 2) NOT NULL DEFAULT 0,
    assigned_customer_count  INT            NOT NULL DEFAULT 0,
    active_customer_count    INT            NOT NULL DEFAULT 0,
    orders_count             INT            NOT NULL DEFAULT 0,
    total_order_value        DECIMAL(12, 2) NOT NULL DEFAULT 0,
    average_order_value      DECIMAL(12, 2) NOT NULL DEFAULT 0,
    new_customers_count      INT            NOT NULL DEFAULT 0,
    repeat_customers_count   INT            NOT NULL DEFAULT 0,
    recommendation_code      VARCHAR(40),
    status                   VARCHAR(20)    NOT NULL DEFAULT 'DRAFT',
    engine_version           VARCHAR(20)    NOT NULL,
    generated_by             VARCHAR(50),
    generated_date           DATETIME       NOT NULL,
    approved_by              VARCHAR(50),
    approved_date            DATETIME,
    PRIMARY KEY (id),
    CONSTRAINT fk_perf_snapshot_employee FOREIGN KEY (employee_id) REFERENCES employees (id),
    CONSTRAINT fk_perf_snapshot_payroll_run FOREIGN KEY (payroll_run_id) REFERENCES payroll_runs (id),
    CONSTRAINT fk_perf_snapshot_sales_policy FOREIGN KEY (sales_policy_id) REFERENCES employee_sales_policies (id),
    CONSTRAINT fk_perf_snapshot_incentive_slab FOREIGN KEY (incentive_slab_id) REFERENCES incentive_slabs (id),
    UNIQUE KEY uq_perf_snapshot_employee_period (employee_id, period_year, period_month),
    KEY idx_perf_snapshot_payroll_run (payroll_run_id),
    KEY idx_perf_snapshot_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;