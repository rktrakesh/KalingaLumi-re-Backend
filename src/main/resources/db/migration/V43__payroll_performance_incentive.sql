-- V43__payroll_performance_incentive.sql
-- The one Payroll <-> Performance integration point (read-only, one-way): Payroll stores
-- the approved incentive amount it read from PerformanceSnapshotService at generation
-- time. Payroll never calculates this value itself and never writes back to the
-- Performance Engine's tables.

ALTER TABLE payroll_details
    ADD COLUMN performance_incentive_amount DECIMAL(12, 2) NOT NULL DEFAULT 0;

ALTER TABLE payroll_calculation_logs
    ADD COLUMN performance_incentive_amount DECIMAL(12, 2) NOT NULL DEFAULT 0;