-- V34__payroll_calculation_log_engine_version.sql
-- Records which PayrollCalculationEngine version produced each calculation log row, so
-- a future change to the engine's formulas can never be silently mistaken for having
-- applied to historical payroll (item 4, Payroll Calculation Versioning).

ALTER TABLE payroll_calculation_logs
    ADD COLUMN engine_version VARCHAR(10) NULL;