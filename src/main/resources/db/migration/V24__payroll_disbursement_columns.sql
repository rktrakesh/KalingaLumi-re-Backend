-- Add salary disbursement tracking columns to payroll_details
ALTER TABLE payroll_details
    ADD COLUMN payment_mode VARCHAR(20) NULL COMMENT 'CASH or BANK',
    ADD COLUMN paid_by VARCHAR(50) NULL COMMENT 'Username who disbursed';

-- change the transaction_type column in inventory_ledger to VARCHAR(50) to accommodate longer descriptions
ALTER TABLE inventory_ledger
    MODIFY transaction_type VARCHAR(50);