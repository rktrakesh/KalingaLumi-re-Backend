-- V11__create_suppliers_and_customers.sql

CREATE TABLE suppliers
(
    id                 BIGINT       NOT NULL AUTO_INCREMENT,
    supplier_code      VARCHAR(20)  NOT NULL,
    name               VARCHAR(200) NOT NULL,
    phone              VARCHAR(20),
    address            TEXT,
    materials_supplied TEXT,
    status             VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_by         VARCHAR(50),
    created_date       DATETIME     NOT NULL,
    updated_by         VARCHAR(50),
    updated_date       DATETIME,
    PRIMARY KEY (id),
    UNIQUE KEY uq_supplier_code (supplier_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE supplier_ledger
(
    id               BIGINT         NOT NULL AUTO_INCREMENT,
    supplier_id      BIGINT         NOT NULL,
    transaction_date DATE           NOT NULL,
    transaction_type VARCHAR(20) NOT NULL,
    amount           DECIMAL(12, 2) NOT NULL,
    balance_after    DECIMAL(12, 2) NOT NULL,
    reference_id     BIGINT,
    remarks          TEXT,
    created_by       VARCHAR(50)    NOT NULL,
    created_date     DATETIME       NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_sup_ledger_supplier FOREIGN KEY (supplier_id) REFERENCES suppliers (id),
    KEY              idx_sup_ledger_supplier (supplier_id),
    KEY              idx_sup_ledger_date (transaction_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE customers
(
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    customer_code VARCHAR(20)  NOT NULL,
    name          VARCHAR(200) NOT NULL,
    phone         VARCHAR(20),
    address       TEXT,
    gst_number    VARCHAR(20),
    credit_days   INT          NOT NULL DEFAULT 30,
    status        VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_by    VARCHAR(50),
    created_date  DATETIME     NOT NULL,
    updated_by    VARCHAR(50),
    updated_date  DATETIME,
    PRIMARY KEY (id),
    UNIQUE KEY uq_customer_code (customer_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE customer_ledger
(
    id               BIGINT         NOT NULL AUTO_INCREMENT,
    customer_id      BIGINT         NOT NULL,
    transaction_date DATE           NOT NULL,
    transaction_type VARCHAR(20) NOT NULL,
    amount           DECIMAL(12, 2) NOT NULL,
    balance_after    DECIMAL(12, 2) NOT NULL,
    due_date         DATE,
    reference_id     BIGINT,
    remarks          TEXT,
    created_by       VARCHAR(50)    NOT NULL,
    created_date     DATETIME       NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_cust_ledger_customer FOREIGN KEY (customer_id) REFERENCES customers (id),
    KEY              idx_cust_ledger_customer (customer_id),
    KEY              idx_cust_ledger_date (transaction_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE seq_supplier
(
    next_val BIGINT NOT NULL DEFAULT 1
) ENGINE=InnoDB;
INSERT INTO seq_supplier
VALUES (1);

CREATE TABLE seq_customer
(
    next_val BIGINT NOT NULL DEFAULT 1
) ENGINE=InnoDB;
INSERT INTO seq_customer
VALUES (1);
