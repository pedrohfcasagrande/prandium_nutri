-- V011: timestamps do usuario + tabela de audit log para Fase 4 (metricas / trilha)

ALTER TABLE users
    ADD COLUMN created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ADD COLUMN last_seen_at DATETIME NULL;

CREATE INDEX idx_users_created_at ON users (created_at);
CREATE INDEX idx_users_last_seen_at ON users (last_seen_at);

CREATE TABLE audit_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    actor_user_id BIGINT NULL,           -- pode ser NULL apos LGPD purge
    actor_role VARCHAR(20) NULL,
    on_behalf_of_user_id BIGINT NULL,    -- alvo (impersonation)
    company_context_id BIGINT NULL,
    action VARCHAR(60) NOT NULL,
    entity_type VARCHAR(60) NULL,
    entity_id BIGINT NULL,
    metadata VARCHAR(2000) NULL,         -- JSON serializado como string (portavel H2/MySQL)
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_audit_actor (actor_user_id, created_at),
    INDEX idx_audit_target (on_behalf_of_user_id, created_at),
    INDEX idx_audit_action (action, created_at),
    INDEX idx_audit_company (company_context_id, created_at)
) ENGINE=InnoDB;
