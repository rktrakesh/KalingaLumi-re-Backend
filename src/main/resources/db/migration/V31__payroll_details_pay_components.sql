-- V31__payroll_details_pay_components.sql
-- Extends payroll_details with the new pay components required by the payroll
-- calendar (weekly-off pay, holiday OT, leave encashment, loss-of-pay) and the day
-- counts behind them, plus the calculation version this detail row belongs to.

ALTER TABLE payroll_details
    ADD COLUMN calculation_version INT NOT NULL DEFAULT 1,

    ADD COLUMN present_days INT NOT NULL DEFAULT 0,
    ADD COLUMN weekly_off_days INT NOT NULL DEFAULT 0,
    ADD COLUMN weekly_off_worked_days INT NOT NULL DEFAULT 0,
    ADD COLUMN holiday_days INT NOT NULL DEFAULT 0,
    ADD COLUMN holiday_worked_days INT NOT NULL DEFAULT 0,
    ADD COLUMN automatic_paid_leave_days INT NOT NULL DEFAULT 0,
    ADD COLUMN absent_days INT NOT NULL DEFAULT 0,

    ADD COLUMN holiday_ot_minutes INT NOT NULL DEFAULT 0,
    ADD COLUMN weekly_off_ot_minutes INT NOT NULL DEFAULT 0,
    ADD COLUMN weekly_off_multiplier DECIMAL(6, 3) NOT NULL DEFAULT 1.5,
    ADD COLUMN holiday_ot_multiplier DECIMAL(6, 3) NOT NULL DEFAULT 2.0,

    ADD COLUMN weekly_off_pay DECIMAL(12, 2) NOT NULL DEFAULT 0,
    ADD COLUMN holiday_ot_pay DECIMAL(12, 2) NOT NULL DEFAULT 0,
    ADD COLUMN leave_encashment_days DECIMAL(6, 2) NOT NULL DEFAULT 0,
    ADD COLUMN leave_encashment_amount DECIMAL(12, 2) NOT NULL DEFAULT 0,
    ADD COLUMN loss_of_pay_amount DECIMAL(12, 2) NOT NULL DEFAULT 0;

-- payroll_details previously required exactly one row per (run, employee). Historical
-- versions are now separate payroll_runs rows, so this uniqueness still holds per run
-- and needs no change.