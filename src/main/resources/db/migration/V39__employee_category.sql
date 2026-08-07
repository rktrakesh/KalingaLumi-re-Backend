-- V39__employee_category.sql
-- Introduces a proper EmployeeCategory (FACTORY / SALES / ADMINISTRATION) so downstream
-- engines (Overtime eligibility, Payroll, the new Performance Engine) can branch on a
-- real column instead of the free-text `designation`. Backfilled to FACTORY for every
-- existing employee so today's behaviour (overtime-eligible, no performance tracking)
-- is unchanged until someone is explicitly re-categorised as SALES/ADMINISTRATION.

ALTER TABLE employees
    ADD COLUMN employee_category VARCHAR(30) NULL;

UPDATE employees SET employee_category = 'FACTORY' WHERE employee_category IS NULL;

ALTER TABLE employees
    MODIFY COLUMN employee_category VARCHAR(30) NOT NULL;