-- V15__create_sales.sql

CREATE TABLE sales_invoices
(
    id                 BIGINT         NOT NULL AUTO_INCREMENT,
    invoice_reference  VARCHAR(30)    NOT NULL,
    customer_id        BIGINT         NOT NULL,
    invoice_date       DATE           NOT NULL,
    due_date           DATE           NOT NULL,
    total_amount       DECIMAL(12, 2) NOT NULL,
    paid_amount        DECIMAL(12, 2) NOT NULL DEFAULT 0,
    outstanding_amount DECIMAL(12, 2) NOT NULL,
    payment_status     VARCHAR(20) NOT NULL DEFAULT 'UNPAID',
    status             VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    remarks            TEXT,
    created_by         VARCHAR(50),
    created_date       DATETIME       NOT NULL,
    updated_by         VARCHAR(50),
    updated_date       DATETIME,
    PRIMARY KEY (id),
    UNIQUE KEY uq_invoice_reference (invoice_reference),
    CONSTRAINT fk_invoice_customer FOREIGN KEY (customer_id) REFERENCES customers (id),
    KEY                idx_invoice_customer (customer_id),
    KEY                idx_invoice_date (invoice_date),
    KEY                idx_invoice_due_date (due_date),
    KEY                idx_invoice_payment_status (payment_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE sales_invoice_items
(
    id           BIGINT         NOT NULL AUTO_INCREMENT,
    invoice_id   BIGINT         NOT NULL,
    material_id  BIGINT         NOT NULL,
    quantity_kg  DECIMAL(12, 3) NOT NULL,
    unit_rate    DECIMAL(12, 4) NOT NULL,
    total_amount DECIMAL(12, 2) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_sii_invoice FOREIGN KEY (invoice_id) REFERENCES sales_invoices (id),
    CONSTRAINT fk_sii_material FOREIGN KEY (material_id) REFERENCES materials (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE sales_payments
(
    id           BIGINT         NOT NULL AUTO_INCREMENT,
    invoice_id   BIGINT         NOT NULL,
    payment_date DATE           NOT NULL,
    amount       DECIMAL(12, 2) NOT NULL,
    payment_mode VARCHAR(20) NOT NULL,
    remarks      TEXT,
    created_by   VARCHAR(50),
    created_date DATETIME       NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_sp_invoice FOREIGN KEY (invoice_id) REFERENCES sales_invoices (id),
    KEY          idx_sales_payment_date (payment_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE sales_returns
(
    id                BIGINT         NOT NULL AUTO_INCREMENT,
    invoice_id        BIGINT         NOT NULL,
    return_date       DATE           NOT NULL,
    material_id       BIGINT         NOT NULL,
    quantity_returned DECIMAL(12, 3) NOT NULL,
    return_amount     DECIMAL(12, 2) NOT NULL,
    remarks           TEXT,
    created_by        VARCHAR(50),
    created_date      DATETIME       NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_sr_invoice FOREIGN KEY (invoice_id) REFERENCES sales_invoices (id),
    CONSTRAINT fk_sr_material FOREIGN KEY (material_id) REFERENCES materials (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE seq_sale
(
    next_val BIGINT NOT NULL DEFAULT 1
) ENGINE=InnoDB;
INSERT INTO seq_sale
VALUES (1);
