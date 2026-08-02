-- V46__login_audit_logs.sql
-- Records every security-relevant event: login, logout, password change, forgot-password
-- request, password reset, account locked, account unlocked. user_id is nullable — a
-- failed login against a username that doesn't exist still gets logged, with no user to
-- reference.

CREATE TABLE login_audit_logs
(
    id           BIGINT      NOT NULL AUTO_INCREMENT,
    user_id      BIGINT      NULL,
    username     VARCHAR(50) NOT NULL,
    event_type   VARCHAR(30) NOT NULL,
    event_date   DATETIME    NOT NULL,
    ip_address   VARCHAR(45),
    device       VARCHAR(255),
    remarks      VARCHAR(255),
    PRIMARY KEY (id),
    CONSTRAINT fk_login_audit_user FOREIGN KEY (user_id) REFERENCES users (id),
    KEY idx_login_audit_user (user_id),
    KEY idx_login_audit_event_date (event_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;