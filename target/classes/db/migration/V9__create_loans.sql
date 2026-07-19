-- V9__create_loans.sql

CREATE TABLE employee_loans
(
    id                        BIGINT         NOT NULL AUTO_INCREMENT,
    loan_reference            VARCHAR(20)    NOT NULL,
    employee_id               BIGINT         NOT NULL,
    principal_amount          DECIMAL(12, 2) NOT NULL,
    interest_rate             DECIMAL(5, 2)  NOT NULL,
    monthly_interest          DECIMAL(12, 2) NOT NULL,
    monthly_principal_payment DECIMAL(12, 2) NOT NULL,
    status                    VARCHAR(20) NOT NULL DEFAULT 'PENDING_APPROVAL',
    disbursement_date         DATE,
    approved_by               VARCHAR(50),
    approved_date             DATETIME,
    remarks                   TEXT,
    created_by                VARCHAR(50),
    created_date              DATETIME       NOT NULL,
    updated_by                VARCHAR(50),
    updated_date              DATETIME,
    PRIMARY KEY (id),
    UNIQUE KEY uq_loan_reference (loan_reference),
    CONSTRAINT fk_loan_employee FOREIGN KEY (employee_id) REFERENCES employees (id),
    KEY                       idx_loan_employee_status (employee_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Partial unique index: only one ACTIVE loan per employee
CREATE UNIQUE INDEX uq_active_loan_per_employee
    ON employee_loans (
                       employee_id,
        ( CASE WHEN status = 'ACTIVE' THEN 1 ELSE NULL END)
        );
-- Note: MySQL doesn't support partial indexes with WHERE clause natively
-- Enforced at application level via service validation

CREATE TABLE loan_ledger
(
    id               BIGINT         NOT NULL AUTO_INCREMENT,
    loan_id          BIGINT         NOT NULL,
    transaction_date DATE           NOT NULL,
    transaction_type VARCHAR(50) NOT NULL,
    amount           DECIMAL(12, 2) NOT NULL,
    balance_after    DECIMAL(12, 2) NOT NULL,
    reference_id     BIGINT,
    remarks          TEXT,
    created_by       VARCHAR(50)    NOT NULL,
    created_date     DATETIME       NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_loan_ledger_loan FOREIGN KEY (loan_id) REFERENCES employee_loans (id),
    KEY              idx_loan_ledger_loan (loan_id),
    KEY              idx_loan_ledger_date (transaction_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE seq_loan
(
    next_val BIGINT NOT NULL DEFAULT 1
) ENGINE=InnoDB;
INSERT INTO seq_loan
VALUES (1);
