-- V45__password_reset_tokens.sql
-- Forgot Password flow: a secure, single-use, expiring token — never the password itself.

CREATE TABLE password_reset_tokens
(
    id           BIGINT       NOT NULL AUTO_INCREMENT,
    user_id      BIGINT       NOT NULL,
    token        VARCHAR(255) NOT NULL,
    expires_at   DATETIME     NOT NULL,
    used         BOOLEAN      NOT NULL DEFAULT FALSE,
    created_date DATETIME     NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uq_password_reset_token (token),
    CONSTRAINT fk_password_reset_user FOREIGN KEY (user_id) REFERENCES users (id),
    KEY idx_password_reset_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;