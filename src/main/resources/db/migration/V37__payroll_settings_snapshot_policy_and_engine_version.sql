-- V37__payroll_settings_snapshot_policy_and_engine_version.sql
-- Captures which PAYROLL_GENERATION_POLICY was in effect and which PayrollCalculationEngine
-- version produced this snapshot's numbers — both immutable once captured, matching every
-- other field already on this table.

ALTER TABLE payroll_settings_snapshots
    ADD COLUMN generation_policy VARCHAR(30) NULL,
    ADD COLUMN engine_version VARCHAR(10) NULL;