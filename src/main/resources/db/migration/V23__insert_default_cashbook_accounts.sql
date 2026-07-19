-- V23__insert_default_cashbook_accounts.sql
INSERT INTO cashbook_accounts (account_name, account_type, opening_balance, financial_year, status, created_by,
                               created_date)
VALUES ('Main Cash', 'CASH', 0.00, '2024-25', 'ACTIVE', 'SYSTEM', NOW()),
       ('Main Bank Account', 'BANK', 0.00, '2024-25', 'ACTIVE', 'SYSTEM', NOW());
