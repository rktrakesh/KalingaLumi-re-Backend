-- V14__create_production.sql

CREATE TABLE production_batches
(
    id           BIGINT      NOT NULL AUTO_INCREMENT,
    batch_number VARCHAR(30) NOT NULL,
    batch_date   DATE        NOT NULL,
    manager_id   BIGINT,
    status       VARCHAR(20) NOT NULL DEFAULT 'IN_PROGRESS',
    remarks      TEXT,
    created_by   VARCHAR(50),
    created_date DATETIME    NOT NULL,
    updated_by   VARCHAR(50),
    updated_date DATETIME,
    PRIMARY KEY (id),
    UNIQUE KEY uq_batch_number (batch_number),
    CONSTRAINT fk_batch_manager FOREIGN KEY (manager_id) REFERENCES users (id),
    KEY          idx_batch_date (batch_date),
    KEY          idx_batch_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE production_inputs
(
    id            BIGINT         NOT NULL AUTO_INCREMENT,
    batch_id      BIGINT         NOT NULL,
    material_id   BIGINT         NOT NULL,
    quantity_used DECIMAL(12, 3) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_prod_input_batch FOREIGN KEY (batch_id) REFERENCES production_batches (id),
    CONSTRAINT fk_prod_input_material FOREIGN KEY (material_id) REFERENCES materials (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE production_outputs
(
    id                 BIGINT         NOT NULL AUTO_INCREMENT,
    batch_id           BIGINT         NOT NULL,
    material_id        BIGINT         NOT NULL,
    finished_quantity  DECIMAL(12, 3) NOT NULL,
    waste_quantity     DECIMAL(12, 3) NOT NULL DEFAULT 0,
    efficiency_percent DECIMAL(5, 2),
    PRIMARY KEY (id),
    CONSTRAINT fk_prod_output_batch FOREIGN KEY (batch_id) REFERENCES production_batches (id),
    CONSTRAINT fk_prod_output_material FOREIGN KEY (material_id) REFERENCES materials (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE seq_batch
(
    next_val BIGINT NOT NULL DEFAULT 1
) ENGINE=InnoDB;
INSERT INTO seq_batch
VALUES (1);
