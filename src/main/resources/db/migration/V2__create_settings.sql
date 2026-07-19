-- V2__create_settings.sql

CREATE TABLE app_settings
(
    id                  BIGINT       NOT NULL AUTO_INCREMENT,
    setting_key         VARCHAR(100) NOT NULL,
    setting_value       VARCHAR(500) NOT NULL,
    description         VARCHAR(500),
    effective_from_date DATE         NOT NULL,
    created_by          VARCHAR(50),
    created_date        DATETIME     NOT NULL,
    updated_by          VARCHAR(50),
    updated_date        DATETIME,
    PRIMARY KEY (id),
    UNIQUE KEY uq_setting_key (setting_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE app_settings_history
(
    id                  BIGINT       NOT NULL AUTO_INCREMENT,
    setting_key         VARCHAR(100) NOT NULL,
    old_value           VARCHAR(500),
    new_value           VARCHAR(500) NOT NULL,
    effective_from_date DATE         NOT NULL,
    changed_by          VARCHAR(50)  NOT NULL,
    changed_date        DATETIME     NOT NULL,
    PRIMARY KEY (id),
    KEY                 idx_settings_history_key (setting_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
