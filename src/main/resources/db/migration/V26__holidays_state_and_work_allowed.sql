-- V26__holidays_state_and_work_allowed.sql
-- Adds STATE_HOLIDAY support and the work-allowed flag used by the payroll calendar engine.
-- holiday_type is already VARCHAR(20) so no column widening is required for the new value.

ALTER TABLE holidays
    ADD COLUMN work_allowed BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN applicable_state VARCHAR(100) NULL;