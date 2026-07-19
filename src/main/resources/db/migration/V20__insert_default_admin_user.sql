-- V20__insert_default_admin_user.sql
-- Default admin password: Admin@123 (BCrypt hash)
INSERT INTO users (username, password_hash, full_name, role, status, created_by, created_date)
VALUES ('admin', '$2a$12$Gba4P9fwqJdXDfJqN/nNRuQ9zKXkJZ5X8Y7M2pLvR3oHkW1CvBnBe', 'Factory Owner', 'ROLE_ADMIN',
        'ACTIVE', 'SYSTEM', NOW());
