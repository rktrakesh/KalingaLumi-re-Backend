-- V47__password_history.sql
-- Stores previous password hashes (never plain text) so a password change can enforce
-- "can't reuse your last N passwords" — N is configurable via app_settings, not hardcoded.

CREATE TABLE password_history
(
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    user_id       BIGINT       NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    created_date  DATETIME     NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_password_history_user FOREIGN KEY (user_id) REFERENCES users (id),
    KEY idx_password_history_user (user_id, created_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;