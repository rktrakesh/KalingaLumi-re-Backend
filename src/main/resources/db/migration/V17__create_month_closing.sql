-- V17__create_month_closing.sql

CREATE TABLE month_closings
(
    id             BIGINT   NOT NULL AUTO_INCREMENT,
    year           INT      NOT NULL,
    month          INT      NOT NULL,
    status         VARCHAR(20) NOT NULL DEFAULT 'OPEN',
    closed_by      VARCHAR(50),
    closed_date    DATETIME,
    reopened_by    VARCHAR(50),
    reopened_date  DATETIME,
    reopen_remarks TEXT,
    created_by     VARCHAR(50),
    created_date   DATETIME NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uq_month_closing (year, month),
    KEY            idx_month_closing_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
