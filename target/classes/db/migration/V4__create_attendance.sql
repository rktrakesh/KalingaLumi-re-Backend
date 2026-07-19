-- V4__create_attendance.sql

CREATE TABLE attendance_records
(
    id              BIGINT   NOT NULL AUTO_INCREMENT,
    employee_id     BIGINT   NOT NULL,
    attendance_date DATE     NOT NULL,
    check_in        TIME,
    check_out       TIME,
    worked_minutes  INT      NOT NULL DEFAULT 0,
    status          VARCHAR(30) NOT NULL,
    remarks         TEXT,
    created_by      VARCHAR(50),
    created_date    DATETIME NOT NULL,
    updated_by      VARCHAR(50),
    updated_date    DATETIME,
    PRIMARY KEY (id),
    UNIQUE KEY uq_attendance_emp_date (employee_id, attendance_date),
    CONSTRAINT fk_attendance_employee FOREIGN KEY (employee_id) REFERENCES employees (id),
    KEY             idx_attendance_date (attendance_date),
    KEY             idx_attendance_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
