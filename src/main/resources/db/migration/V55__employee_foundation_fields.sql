-- V55__employee_foundation_fields.sql
-- Everything here is purely additive — all nullable, no backfill needed (brand new
-- concepts), existing employees and every existing query/column are unaffected.
--
-- NOTE on designation: the OLD free-text `designation` column (from V3) is deliberately
-- KEPT as-is (legacy/deprecated, no longer written to by new code) rather than dropped —
-- unlike employee_category, this one was not explicitly authorized for full replacement,
-- and free-text values can't be reliably auto-mapped to the new master's codes. New code
-- reads/writes designation_id only.

ALTER TABLE employees
    ADD COLUMN designation_id           BIGINT NULL,
    ADD COLUMN department_id            BIGINT NULL,
    ADD COLUMN employment_type          VARCHAR(20) NULL,
    ADD COLUMN reporting_manager_id     BIGINT NULL,
    ADD COLUMN date_of_birth            DATE NULL,
    ADD COLUMN gender                   VARCHAR(20) NULL,
    ADD COLUMN emergency_contact_name   VARCHAR(100) NULL,
    ADD COLUMN emergency_contact_phone  VARCHAR(20) NULL,
    ADD COLUMN pan_number               VARCHAR(20) NULL,
    ADD COLUMN bank_account_number      VARCHAR(30) NULL,
    ADD COLUMN bank_ifsc                VARCHAR(15) NULL,
    ADD COLUMN bank_name                VARCHAR(100) NULL,
    ADD COLUMN bank_account_holder_name VARCHAR(100) NULL,
    ADD CONSTRAINT fk_employee_designation FOREIGN KEY (designation_id) REFERENCES designation_master (id),
    ADD CONSTRAINT fk_employee_department FOREIGN KEY (department_id) REFERENCES department_master (id),
    ADD CONSTRAINT fk_employee_reporting_manager FOREIGN KEY (reporting_manager_id) REFERENCES employees (id);