-- V6__create_holidays.sql

CREATE TABLE holidays
(
    id           BIGINT       NOT NULL AUTO_INCREMENT,
    holiday_date DATE         NOT NULL,
    name         VARCHAR(200) NOT NULL,
    holiday_type VARCHAR(20) NOT NULL,
    created_by   VARCHAR(50),
    created_date DATETIME     NOT NULL,
    updated_by   VARCHAR(50),
    updated_date DATETIME,
    PRIMARY KEY (id),
    UNIQUE KEY uq_holiday_date (holiday_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
