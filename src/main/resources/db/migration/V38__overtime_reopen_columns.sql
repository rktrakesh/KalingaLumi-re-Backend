-- V38__overtime_reopen_columns.sql
-- Supports reopening an APPROVED/MODIFIED overtime request so attendance underneath it
-- can be corrected. Reopening moves status back to REJECTED (so it no longer counts
-- toward payroll or blocks attendance edits) while preserving a distinct audit trail of
-- who reopened it, when, and why — separate from the original approval fields.

ALTER TABLE overtime_requests
    ADD COLUMN reopened_by VARCHAR(50) NULL,
    ADD COLUMN reopened_date DATETIME NULL,
    ADD COLUMN reopen_reason TEXT NULL;