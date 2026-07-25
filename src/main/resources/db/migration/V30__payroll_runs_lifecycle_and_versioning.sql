-- V30__payroll_runs_lifecycle_and_versioning.sql
-- Expands payroll_runs to support the full DRAFT -> CALCULATED -> VERIFIED -> APPROVED
-- -> PROCESSED -> PAID -> LOCKED lifecycle plus immutable calculation versioning.
--
-- Versioning strategy: a recalculation no longer overwrites the existing run — it
-- inserts a NEW payroll_runs row linked via previous_run_id, and flips
-- is_current_version so exactly one row per (year, month) is "live" at a time.
-- Old versions are retained forever for audit.

ALTER TABLE payroll_runs
    ADD COLUMN calculation_version INT NOT NULL DEFAULT 1,
    ADD COLUMN is_current_version BOOLEAN NOT NULL DEFAULT TRUE,
    ADD COLUMN previous_run_id BIGINT NULL,
    ADD COLUMN snapshot_id BIGINT NULL,

    ADD COLUMN verified_by VARCHAR(50) NULL,
    ADD COLUMN verified_date DATETIME NULL,
    ADD COLUMN approved_by VARCHAR(50) NULL,
    ADD COLUMN approved_date DATETIME NULL,
    ADD COLUMN processed_by VARCHAR(50) NULL,
    ADD COLUMN processed_date DATETIME NULL,
    ADD COLUMN reopened_by VARCHAR(50) NULL,
    ADD COLUMN reopened_date DATETIME NULL,
    ADD COLUMN reopen_reason TEXT NULL;

-- Map legacy statuses onto the new lifecycle vocabulary.
UPDATE payroll_runs SET status = 'CALCULATED' WHERE status IN ('GENERATED', 'REGENERATED');
-- LOCKED rows stay LOCKED.

ALTER TABLE payroll_runs
    ADD CONSTRAINT fk_payroll_run_previous FOREIGN KEY (previous_run_id) REFERENCES payroll_runs (id),
    ADD CONSTRAINT fk_payroll_run_snapshot FOREIGN KEY (snapshot_id) REFERENCES payroll_settings_snapshots (id);

-- Replace the old hard unique(year, month) — multiple historical versions may now share
-- a period. Only one row may be the CURRENT version for a given period at any time,
-- enforced via a generated column that is NULL (and therefore unconstrained) for
-- non-current rows.
ALTER TABLE payroll_runs DROP INDEX uq_payroll_run_month;

ALTER TABLE payroll_runs
    ADD COLUMN current_period_key VARCHAR(20)
        GENERATED ALWAYS AS (CASE WHEN is_current_version THEN CONCAT(year, '-', month) ELSE NULL END) STORED;

CREATE UNIQUE INDEX uq_payroll_run_current_period ON payroll_runs (current_period_key);
CREATE INDEX idx_payroll_run_prev ON payroll_runs (previous_run_id);