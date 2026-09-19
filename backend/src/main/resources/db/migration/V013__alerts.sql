-- V013: Alertas automaticos avaliados diariamente por job.

CREATE TABLE alerts (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    scope VARCHAR(20) NOT NULL,          -- ADMIN, COMPANY, NUTRITIONIST
    scope_id BIGINT NULL,                -- id de company/user quando aplicavel
    severity VARCHAR(20) NOT NULL,       -- INFO, WARN, CRITICAL
    kind VARCHAR(60) NOT NULL,           -- PAYMENT_FAILED, CLIENT_INACTIVE_7D, ...
    title VARCHAR(255) NOT NULL,
    message VARCHAR(1000) NULL,
    metadata VARCHAR(2000) NULL,
    dismissed_at DATETIME NULL,
    dismissed_by_user_id BIGINT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_alerts_scope (scope, scope_id, dismissed_at),
    INDEX idx_alerts_kind (kind, created_at)
) ENGINE=InnoDB;
