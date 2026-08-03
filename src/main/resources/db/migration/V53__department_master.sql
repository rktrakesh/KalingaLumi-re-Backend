-- V53__department_master.sql

CREATE TABLE department_master
(
    id           BIGINT       NOT NULL AUTO_INCREMENT,
    code         VARCHAR(50)  NOT NULL,
    name         VARCHAR(100) NOT NULL,
    description  VARCHAR(255),
    active       BOOLEAN      NOT NULL DEFAULT TRUE,
    created_by   VARCHAR(50),
    created_date DATETIME     NOT NULL,
    updated_by   VARCHAR(50),
    updated_date DATETIME,
    PRIMARY KEY (id),
    UNIQUE KEY uq_department_code (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO department_master (code, name, description, active, created_by, created_date)
VALUES ('PRODUCTION', 'Production', NULL, TRUE, 'SYSTEM', NOW()),
       ('SALES', 'Sales', NULL, TRUE, 'SYSTEM', NOW()),
       ('MARKETING', 'Marketing', NULL, TRUE, 'SYSTEM', NOW()),
       ('FINANCE', 'Finance', NULL, TRUE, 'SYSTEM', NOW()),
       ('HR', 'HR', NULL, TRUE, 'SYSTEM', NOW()),
       ('ADMINISTRATION', 'Administration', NULL, TRUE, 'SYSTEM', NOW());