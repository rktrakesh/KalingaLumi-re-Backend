-- V18__create_audit_and_notifications.sql

CREATE TABLE audit_logs
(
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    username    VARCHAR(50)  NOT NULL,
    module      VARCHAR(50)  NOT NULL,
    action      VARCHAR(100) NOT NULL,
    entity_type VARCHAR(50),
    entity_id   BIGINT,
    old_value   JSON,
    new_value   JSON,
    ip_address  VARCHAR(50),
    created_at  DATETIME     NOT NULL,
    PRIMARY KEY (id),
    KEY         idx_audit_module_date (module, created_at),
    KEY         idx_audit_user_date (username, created_at),
    KEY         idx_audit_entity (entity_type, entity_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE notifications
(
    id                BIGINT       NOT NULL AUTO_INCREMENT,
    user_id           BIGINT       NOT NULL,
    notification_type VARCHAR(40) NOT NULL,
    title             VARCHAR(200) NOT NULL,
    message           TEXT,
    reference_type    VARCHAR(50),
    reference_id      BIGINT,
    status            VARCHAR(20) NOT NULL DEFAULT 'UNREAD',
    created_date      DATETIME     NOT NULL,
    read_date         DATETIME,
    PRIMARY KEY (id),
    CONSTRAINT fk_notif_user FOREIGN KEY (user_id) REFERENCES users (id),
    KEY               idx_notif_user_status (user_id, status),
    KEY               idx_notif_date (created_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
