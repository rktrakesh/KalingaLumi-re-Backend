-- Phase 4: validated employee lifecycle metadata and secure employee documents.
-- Existing employees remain unchanged and ACTIVE employees are grandfathered.

ALTER TABLE employees
    ADD COLUMN notice_start_date DATE NULL AFTER joining_date,
    ADD COLUMN last_working_date DATE NULL AFTER notice_start_date;

CREATE TABLE employee_documents (
    id BIGINT NOT NULL AUTO_INCREMENT,
    employee_id BIGINT NOT NULL,
    document_type VARCHAR(50) NOT NULL,
    original_file_name VARCHAR(255) NOT NULL,
    storage_key VARCHAR(500) NOT NULL,
    mime_type VARCHAR(100) NOT NULL,
    file_size BIGINT NOT NULL,
    expiry_date DATE NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'CURRENT',
    archived_by VARCHAR(50) NULL,
    archived_at DATETIME NULL,
    created_by VARCHAR(50) NULL,
    created_date DATETIME NOT NULL,
    updated_by VARCHAR(50) NULL,
    updated_date DATETIME NULL,
    current_singleton_type VARCHAR(50)
        GENERATED ALWAYS AS (
            CASE
                WHEN status = 'CURRENT' AND document_type <> 'CERTIFICATE' THEN document_type
                ELSE NULL
            END
        ) STORED,
    PRIMARY KEY (id),
    CONSTRAINT fk_employee_document_employee
        FOREIGN KEY (employee_id) REFERENCES employees (id),
    CONSTRAINT uq_employee_document_storage_key UNIQUE (storage_key),
    CONSTRAINT uq_employee_document_current_singleton
        UNIQUE (employee_id, current_singleton_type),
    CONSTRAINT chk_employee_document_type CHECK (document_type IN (
        'PROFILE_PHOTO', 'IDENTITY_PROOF', 'PAN', 'BANK_PROOF', 'CERTIFICATE',
        'DRIVING_LICENSE', 'APPOINTMENT_LETTER', 'SALARY_SLIP_ACKNOWLEDGEMENT'
    )),
    CONSTRAINT chk_employee_document_status
        CHECK (status IN ('CURRENT', 'SUPERSEDED', 'ARCHIVED')),
    CONSTRAINT chk_employee_document_size CHECK (file_size > 0),
    KEY idx_employee_document_employee (employee_id),
    KEY idx_employee_document_lookup (employee_id, document_type, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
