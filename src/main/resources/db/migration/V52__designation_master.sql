-- V52__designation_master.sql
-- Every designation belongs to exactly one Employee Category. Seeded with a starting set
-- from the ERP's own spec examples — HR can add more from the UI without any code change.

CREATE TABLE designation_master
(
    id           BIGINT      NOT NULL AUTO_INCREMENT,
    code         VARCHAR(50) NOT NULL,
    name         VARCHAR(100) NOT NULL,
    category_id  BIGINT      NOT NULL,
    description  VARCHAR(255),
    active       BOOLEAN     NOT NULL DEFAULT TRUE,
    created_by   VARCHAR(50),
    created_date DATETIME    NOT NULL,
    updated_by   VARCHAR(50),
    updated_date DATETIME,
    PRIMARY KEY (id),
    UNIQUE KEY uq_designation_code (code),
    CONSTRAINT fk_designation_category FOREIGN KEY (category_id) REFERENCES employee_category_master (id),
    KEY idx_designation_category (category_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO designation_master (code, name, category_id, description, active, created_by, created_date)
SELECT 'MACHINE_OPERATOR', 'Machine Operator', id, NULL, TRUE, 'SYSTEM', NOW() FROM employee_category_master WHERE code = 'FACTORY'
UNION ALL SELECT 'SUPERVISOR', 'Supervisor', id, NULL, TRUE, 'SYSTEM', NOW() FROM employee_category_master WHERE code = 'FACTORY'
UNION ALL SELECT 'HELPER', 'Helper', id, NULL, TRUE, 'SYSTEM', NOW() FROM employee_category_master WHERE code = 'FACTORY'
UNION ALL SELECT 'SALES_EXECUTIVE', 'Sales Executive', id, NULL, TRUE, 'SYSTEM', NOW() FROM employee_category_master WHERE code = 'SALES'
UNION ALL SELECT 'MARKETING_EXECUTIVE', 'Marketing Executive', id, NULL, TRUE, 'SYSTEM', NOW() FROM employee_category_master WHERE code = 'SALES'
UNION ALL SELECT 'AREA_SALES_MANAGER', 'Area Sales Manager', id, NULL, TRUE, 'SYSTEM', NOW() FROM employee_category_master WHERE code = 'SALES'
UNION ALL SELECT 'STORE_KEEPER', 'Store Keeper', id, NULL, TRUE, 'SYSTEM', NOW() FROM employee_category_master WHERE code = 'ADMINISTRATION'
UNION ALL SELECT 'ACCOUNTANT', 'Accountant', id, NULL, TRUE, 'SYSTEM', NOW() FROM employee_category_master WHERE code = 'ADMINISTRATION'
UNION ALL SELECT 'HR_EXECUTIVE', 'HR Executive', id, NULL, TRUE, 'SYSTEM', NOW() FROM employee_category_master WHERE code = 'ADMINISTRATION'
UNION ALL SELECT 'GENERAL_MANAGER', 'General Manager', id, NULL, TRUE, 'SYSTEM', NOW() FROM employee_category_master WHERE code = 'MANAGEMENT'
UNION ALL SELECT 'FACTORY_OWNER', 'Factory Owner', id, NULL, TRUE, 'SYSTEM', NOW() FROM employee_category_master WHERE code = 'MANAGEMENT';