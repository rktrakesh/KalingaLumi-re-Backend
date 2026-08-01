-- V41__performance_engine_foundation.sql
-- Foundation tables for the Performance Engine (Sales & Marketing). Deliberately owned
-- entirely by this engine — nothing here modifies Payroll, Customer, or Sales schemas
-- beyond the two additive columns already added in V40. Targets and commission slabs
-- are NEVER stored on Employee; ownership is NEVER stored as a single column on
-- Customer — both are versioned/historical tables so past performance is never
-- retroactively rewritten by a later policy or ownership change.

-- Versioned sales policy per employee. Never overwritten — a change creates a new row
-- and supersedes the previous one (identical pattern to employee_salary_history).
CREATE TABLE employee_sales_policies
(
    id              BIGINT         NOT NULL AUTO_INCREMENT,
    employee_id     BIGINT         NOT NULL,
    monthly_target  DECIMAL(12, 2) NOT NULL,
    effective_from  DATE           NOT NULL,
    effective_to    DATE           NULL,
    version         INT            NOT NULL,
    status          VARCHAR(20)    NOT NULL DEFAULT 'ACTIVE',
    created_by      VARCHAR(50),
    created_date    DATETIME       NOT NULL,
    updated_by      VARCHAR(50),
    updated_date    DATETIME,
    PRIMARY KEY (id),
    CONSTRAINT fk_sales_policy_employee FOREIGN KEY (employee_id) REFERENCES employees (id),
    KEY idx_sales_policy_employee_status (employee_id, status),
    KEY idx_sales_policy_effective (employee_id, effective_from, effective_to)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Configurable incentive slabs owned by one sales policy version. max_achievement_pct
-- NULL means unbounded ("Above 150%" style slabs). Never a single flat percentage.
CREATE TABLE incentive_slabs
(
    id                   BIGINT         NOT NULL AUTO_INCREMENT,
    sales_policy_id      BIGINT         NOT NULL,
    min_achievement_pct  DECIMAL(6, 2)  NOT NULL,
    max_achievement_pct  DECIMAL(6, 2)  NULL,
    incentive_pct        DECIMAL(5, 2)  NOT NULL,
    slab_order           INT            NOT NULL,
    created_by           VARCHAR(50),
    created_date         DATETIME       NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_incentive_slab_policy FOREIGN KEY (sales_policy_id) REFERENCES employee_sales_policies (id),
    KEY idx_incentive_slab_policy_order (sales_policy_id, slab_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Who owns each customer, over time. A customer can have many rows historically but at
-- most one ACTIVE row at any moment (enforced in service layer, not a DB constraint,
-- since MySQL cannot express "one active row per customer" declaratively).
CREATE TABLE customer_ownership
(
    id             BIGINT      NOT NULL AUTO_INCREMENT,
    customer_id    BIGINT      NOT NULL,
    employee_id    BIGINT      NOT NULL,
    effective_from DATE        NOT NULL,
    effective_to   DATE        NULL,
    is_temporary   BOOLEAN     NOT NULL DEFAULT FALSE,
    status         VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    remarks        VARCHAR(255),
    created_by     VARCHAR(50),
    created_date   DATETIME    NOT NULL,
    updated_by     VARCHAR(50),
    updated_date   DATETIME,
    PRIMARY KEY (id),
    CONSTRAINT fk_customer_ownership_customer FOREIGN KEY (customer_id) REFERENCES customers (id),
    CONSTRAINT fk_customer_ownership_employee FOREIGN KEY (employee_id) REFERENCES employees (id),
    KEY idx_customer_ownership_customer_status (customer_id, status),
    KEY idx_customer_ownership_employee (employee_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- CRM-style visit log — recorded even when no order results, so "Suresh visited but
-- Rahul owns the customer" is auditable independently of the Sales Credit Rule.
CREATE TABLE customer_visits
(
    id                     BIGINT      NOT NULL AUTO_INCREMENT,
    customer_id            BIGINT      NOT NULL,
    visited_by_employee_id BIGINT      NOT NULL,
    visit_date             DATE        NOT NULL,
    visit_purpose          VARCHAR(30) NOT NULL,
    visit_outcome          VARCHAR(30) NOT NULL,
    remarks                TEXT,
    created_by             VARCHAR(50),
    created_date           DATETIME    NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_customer_visit_customer FOREIGN KEY (customer_id) REFERENCES customers (id),
    CONSTRAINT fk_customer_visit_employee FOREIGN KEY (visited_by_employee_id) REFERENCES employees (id),
    KEY idx_customer_visit_customer_date (customer_id, visit_date),
    KEY idx_customer_visit_employee_date (visited_by_employee_id, visit_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;