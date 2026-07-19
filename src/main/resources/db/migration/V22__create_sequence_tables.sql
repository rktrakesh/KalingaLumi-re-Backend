-- V22__create_sequence_tables.sql
-- Sequence tables for reference numbers not yet created in prior migrations
INSERT INTO seq_sale
VALUES (1);

CREATE TABLE seq_payroll
(
    next_val BIGINT NOT NULL DEFAULT 1
) ENGINE=InnoDB;
INSERT INTO seq_payroll
VALUES (1);
