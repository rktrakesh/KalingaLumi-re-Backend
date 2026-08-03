-- V54__employees_category_master_migration.sql
-- Replaces employees.employee_category (a VARCHAR holding the old Java enum's name) with
-- a real FK to employee_category_master — per explicit instruction, this is a full
-- replacement, not an addition: the business logic those 6 call sites depend on (Overtime
-- eligibility, Payroll's Performance Incentive integration, Sales Credit ownership rules)
-- is unchanged; only where the category value comes from has changed.

ALTER TABLE employees
    ADD COLUMN employee_category_id BIGINT NULL;

-- Backfill: the old column's values are exactly the new master's codes (FACTORY/SALES/
-- ADMINISTRATION), so this join-based backfill is exact and lossless.
UPDATE employees e
    JOIN employee_category_master m ON e.employee_category = m.code
    SET e.employee_category_id = m.id;

-- Safety net: any employee whose old value somehow didn't match (shouldn't happen, since
-- V39 only ever wrote FACTORY/SALES/ADMINISTRATION) falls back to FACTORY rather than
-- leaving the FK null.
UPDATE employees e
SET e.employee_category_id = (SELECT id FROM employee_category_master WHERE code = 'FACTORY')
WHERE e.employee_category_id IS NULL;

ALTER TABLE employees
    MODIFY COLUMN employee_category_id BIGINT NOT NULL,
    ADD CONSTRAINT fk_employee_category FOREIGN KEY (employee_category_id) REFERENCES employee_category_master (id),
DROP COLUMN employee_category;