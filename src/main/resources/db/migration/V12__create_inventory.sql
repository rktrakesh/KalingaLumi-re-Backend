-- V12__create_inventory.sql

CREATE TABLE materials
(
    id            BIGINT         NOT NULL AUTO_INCREMENT,
    material_code VARCHAR(20)    NOT NULL,
    name          VARCHAR(200)   NOT NULL,
    unit          VARCHAR(20) NOT NULL,
    reorder_level DECIMAL(12, 3) NOT NULL DEFAULT 0,
    material_type VARCHAR(20) NOT NULL,
    status        VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_by    VARCHAR(50),
    created_date  DATETIME       NOT NULL,
    updated_by    VARCHAR(50),
    updated_date  DATETIME,
    PRIMARY KEY (id),
    UNIQUE KEY uq_material_code (material_code),
    KEY           idx_material_type (material_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE inventory_ledger
(
    id               BIGINT         NOT NULL AUTO_INCREMENT,
    material_id      BIGINT         NOT NULL,
    transaction_date DATE           NOT NULL,
    transaction_type VARCHAR(20) NOT NULL,
    quantity         DECIMAL(12, 3) NOT NULL,
    balance_after    DECIMAL(12, 3) NOT NULL,
    unit_cost        DECIMAL(12, 4),
    reference_type   VARCHAR(50),
    reference_id     BIGINT,
    remarks          TEXT,
    created_by       VARCHAR(50)    NOT NULL,
    created_date     DATETIME       NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_inv_ledger_material FOREIGN KEY (material_id) REFERENCES materials (id),
    KEY              idx_inv_ledger_material (material_id),
    KEY              idx_inv_ledger_date (transaction_date),
    KEY              idx_inv_ledger_type (transaction_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE seq_material
(
    next_val BIGINT NOT NULL DEFAULT 1
) ENGINE=InnoDB;
INSERT INTO seq_material
VALUES (1);
