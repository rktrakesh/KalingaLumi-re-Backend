-- V16__create_cashbook.sql

CREATE TABLE cashbook_accounts
(
    id              BIGINT         NOT NULL AUTO_INCREMENT,
    account_name    VARCHAR(100)   NOT NULL,
    account_type    VARCHAR(20)    NOT NULL,
    opening_balance DECIMAL(15, 2) NOT NULL DEFAULT 0,
    financial_year  VARCHAR(10)    NOT NULL,
    status          VARCHAR(20)    NOT NULL DEFAULT 'ACTIVE',
    created_by      VARCHAR(50),
    created_date    DATETIME       NOT NULL,
    updated_by      VARCHAR(50),
    updated_date    DATETIME,
    PRIMARY KEY (id),
    KEY             idx_cashbook_account_type (account_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE cashbook_transactions
(
    id               BIGINT         NOT NULL AUTO_INCREMENT,
    account_id       BIGINT         NOT NULL,
    transaction_date DATE           NOT NULL,
    transaction_type VARCHAR(20)    NOT NULL,
    flow_type        VARCHAR(20)    NOT NULL,
    amount           DECIMAL(12, 2) NOT NULL,
    balance_after    DECIMAL(15, 2) NOT NULL,
    reference_type   VARCHAR(50),
    reference_id     BIGINT,
    description      TEXT,
    created_by       VARCHAR(50)    NOT NULL,
    created_date     DATETIME       NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_cbt_account FOREIGN KEY (account_id) REFERENCES cashbook_accounts (id),
    KEY              idx_cashbook_account_date (account_id, transaction_date),
    KEY              idx_cashbook_date (transaction_date),
    KEY              idx_cashbook_type (transaction_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
