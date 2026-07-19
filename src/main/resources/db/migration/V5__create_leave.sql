-- V5__create_leave.sql

CREATE TABLE leave_balances
(
    id          BIGINT NOT NULL AUTO_INCREMENT,
    employee_id BIGINT NOT NULL,
    year        INT    NOT NULL,
    month       INT    NOT NULL,
    allocated   INT    NOT NULL,
    used        INT    NOT NULL DEFAULT 0,
    balance     INT    NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uq_leave_balance_emp_month (employee_id, year, month),
    CONSTRAINT fk_leave_bal_employee FOREIGN KEY (employee_id) REFERENCES employees (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE leave_requests
(
    id               BIGINT   NOT NULL AUTO_INCREMENT,
    employee_id      BIGINT   NOT NULL,
    leave_date       DATE     NOT NULL,
    reason           TEXT,
    status           VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    approved_by      VARCHAR(50),
    approved_date    DATETIME,
    rejection_reason TEXT,
    created_by       VARCHAR(50),
    created_date     DATETIME NOT NULL,
    updated_by       VARCHAR(50),
    updated_date     DATETIME,
    PRIMARY KEY (id),
    CONSTRAINT fk_leave_req_employee FOREIGN KEY (employee_id) REFERENCES employees (id),
    KEY              idx_leave_req_emp_status (employee_id, status),
    KEY              idx_leave_req_date (leave_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
