-- V7__create_overtime.sql

CREATE TABLE overtime_requests
(
    id                BIGINT   NOT NULL AUTO_INCREMENT,
    employee_id       BIGINT   NOT NULL,
    attendance_id     BIGINT,
    overtime_date     DATE     NOT NULL,
    request_type      VARCHAR(20) NOT NULL,
    requested_minutes INT      NOT NULL,
    approved_minutes  INT,
    status            VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    approved_by       VARCHAR(50),
    approved_date     DATETIME,
    remarks           TEXT,
    created_by        VARCHAR(50),
    created_date      DATETIME NOT NULL,
    updated_by        VARCHAR(50),
    updated_date      DATETIME,
    PRIMARY KEY (id),
    CONSTRAINT fk_ot_employee FOREIGN KEY (employee_id) REFERENCES employees (id),
    CONSTRAINT fk_ot_attendance FOREIGN KEY (attendance_id) REFERENCES attendance_records (id),
    KEY               idx_ot_emp_status (employee_id, status),
    KEY               idx_ot_date (overtime_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
