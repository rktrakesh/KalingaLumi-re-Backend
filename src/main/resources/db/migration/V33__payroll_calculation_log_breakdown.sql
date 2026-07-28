-- V33__payroll_calculation_log_breakdown.sql
-- Human-readable, line-by-line explanation of how each pay component was computed
-- (e.g. "Overtime: 34.0 Hours x Rs.49.4500 x 1.5x = Rs.2521.95"), stored verbatim
-- alongside the numeric log so payroll disputes can be explained without recomputation.

ALTER TABLE payroll_calculation_logs
    ADD COLUMN calculation_breakdown TEXT NULL;