-- V44__users_employee_link_and_security_columns.sql
-- The User<->Employee relationship already existed as a bare `employee_id` column with
-- no constraint — nothing stopped two Users pointing at the same Employee. This makes it
-- a true 1:1: a UNIQUE constraint (MySQL allows multiple NULLs through a UNIQUE index,
-- so ADMIN/system accounts with no Employee are unaffected) plus a proper FK.
--
-- IMPORTANT — verify before running: if any duplicate non-null employee_id values already
-- exist in your `users` table, this migration will fail. Run this check first:
--   SELECT employee_id, COUNT(*) FROM users WHERE employee_id IS NOT NULL
--   GROUP BY employee_id HAVING COUNT(*) > 1;
-- and resolve any duplicates before deploying.

ALTER TABLE users
    ADD CONSTRAINT uq_users_employee_id UNIQUE (employee_id),
    ADD CONSTRAINT fk_users_employee FOREIGN KEY (employee_id) REFERENCES employees (id);

-- New security columns for the IAM extension. Existing users are grandfathered with
-- must_change_password = FALSE (nobody already logged-in is suddenly forced to change
-- their password); only newly onboarded users get TRUE from the onboarding service.
ALTER TABLE users
    ADD COLUMN must_change_password BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN failed_login_attempts INT NOT NULL DEFAULT 0,
    ADD COLUMN locked_at DATETIME NULL;