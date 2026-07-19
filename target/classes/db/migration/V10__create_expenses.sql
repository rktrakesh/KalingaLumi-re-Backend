-- V10__create_expenses.sql

CREATE TABLE expenses
(
    id                BIGINT         NOT NULL AUTO_INCREMENT,
    expense_reference VARCHAR(30)    NOT NULL,
    expense_date      DATE           NOT NULL,
    amount            DECIMAL(12, 2) NOT NULL,
    category          VARCHAR(20) NOT NULL,
    remarks           TEXT,
    status            VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    approved_by       VARCHAR(50),
    approved_date     DATETIME,
    created_by        VARCHAR(50),
    created_date      DATETIME       NOT NULL,
    updated_by        VARCHAR(50),
    updated_date      DATETIME,
    PRIMARY KEY (id),
    UNIQUE KEY uq_expense_reference (expense_reference),
    KEY               idx_expense_date_category (expense_date, category),
    KEY               idx_expense_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE seq_expense
(
    next_val BIGINT NOT NULL DEFAULT 1
) ENGINE=InnoDB;
INSERT INTO seq_expense
VALUES (1);
