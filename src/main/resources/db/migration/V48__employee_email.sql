-- V48__employee_email.sql
-- The onboarding flow's Welcome Email and the Forgot Password flow's reset link both need
-- somewhere to send to — Employee had no email address stored anywhere. Nullable: existing
-- employees are unaffected until HR fills theirs in.

ALTER TABLE employees
    ADD COLUMN email VARCHAR(150) NULL;