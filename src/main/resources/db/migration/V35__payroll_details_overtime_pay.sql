-- V35__payroll_details_overtime_pay.sql
-- weekly_off_pay and holiday_ot_pay are already stored directly; overtime_pay was the
-- one pay component still being re-derived from raw fields wherever it was needed
-- (e.g. the dashboard). Store it once, like its siblings, so nothing downstream ever
-- has to recompute a PayrollCalculationEngine formula outside the engine itself.

ALTER TABLE payroll_details
    ADD COLUMN overtime_pay DECIMAL(12, 2) NOT NULL DEFAULT 0;