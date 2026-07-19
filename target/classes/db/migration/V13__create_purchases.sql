-- V13__create_purchases.sql

CREATE TABLE purchases
(
    id                 BIGINT         NOT NULL AUTO_INCREMENT,
    purchase_reference VARCHAR(30)    NOT NULL,
    supplier_id        BIGINT         NOT NULL,
    purchase_date      DATE           NOT NULL,
    total_amount       DECIMAL(12, 2) NOT NULL,
    paid_amount        DECIMAL(12, 2) NOT NULL DEFAULT 0,
    outstanding_amount DECIMAL(12, 2) NOT NULL,
    payment_status     VARCHAR(20) NOT NULL DEFAULT 'UNPAID',
    remarks            TEXT,
    status             VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_by         VARCHAR(50),
    created_date       DATETIME       NOT NULL,
    updated_by         VARCHAR(50),
    updated_date       DATETIME,
    PRIMARY KEY (id),
    UNIQUE KEY uq_purchase_reference (purchase_reference),
    CONSTRAINT fk_purchase_supplier FOREIGN KEY (supplier_id) REFERENCES suppliers (id),
    KEY                idx_purchase_supplier (supplier_id),
    KEY                idx_purchase_date (purchase_date),
    KEY                idx_purchase_status (payment_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE purchase_items
(
    id           BIGINT         NOT NULL AUTO_INCREMENT,
    purchase_id  BIGINT         NOT NULL,
    material_id  BIGINT         NOT NULL,
    quantity     DECIMAL(12, 3) NOT NULL,
    unit_rate    DECIMAL(12, 4) NOT NULL,
    total_amount DECIMAL(12, 2) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_pi_purchase FOREIGN KEY (purchase_id) REFERENCES purchases (id),
    CONSTRAINT fk_pi_material FOREIGN KEY (material_id) REFERENCES materials (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE purchase_payments
(
    id           BIGINT         NOT NULL AUTO_INCREMENT,
    purchase_id  BIGINT         NOT NULL,
    payment_date DATE           NOT NULL,
    amount       DECIMAL(12, 2) NOT NULL,
    payment_mode VARCHAR(20) NOT NULL,
    remarks      TEXT,
    created_by   VARCHAR(50),
    created_date DATETIME       NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_pp_purchase FOREIGN KEY (purchase_id) REFERENCES purchases (id),
    KEY          idx_purchase_payment_date (payment_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE seq_purchase
(
    next_val BIGINT NOT NULL DEFAULT 1
) ENGINE=InnoDB;
INSERT INTO seq_purchase
VALUES (1);
