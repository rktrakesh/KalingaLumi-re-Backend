-- V25__attendance_payroll_lock.sql
-- Adds payroll-freeze support to attendance_records. Attendance FACTS are never
-- altered here — this only adds a lock flag consulted by the attendance service.

ALTER TABLE attendance_records
    ADD COLUMN locked_for_payroll BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN locked_by_payroll_run_id BIGINT NULL;

CREATE INDEX idx_attendance_locked ON attendance_records (locked_for_payroll);
CREATE INDEX idx_attendance_locked_run ON attendance_records (locked_by_payroll_run_id);