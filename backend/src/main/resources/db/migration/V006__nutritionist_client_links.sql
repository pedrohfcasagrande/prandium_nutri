-- V006: Vinculo entre Nutricionista (user com role=NUTRITIONIST) e Cliente
-- Fase 2A: apenas nutricionistas independentes (sem empresa).

CREATE TABLE nutritionist_clients (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    nutritionist_id BIGINT NOT NULL,
    client_id BIGINT NULL,                  -- null quando convite ainda nao foi aceito por user existente
    invited_by_email VARCHAR(255) NOT NULL, -- email para quem foi enviado o convite
    invite_token VARCHAR(255) NOT NULL UNIQUE,
    status VARCHAR(30) NOT NULL,            -- PENDING, ACTIVE, REJECTED, REVOKED_BY_CLIENT, REVOKED_BY_NUTRI, EXPIRED
    invited_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    accepted_at DATETIME NULL,
    revoked_at DATETIME NULL,
    expires_at DATETIME NOT NULL,
    CONSTRAINT fk_nc_nutri FOREIGN KEY (nutritionist_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_nc_client FOREIGN KEY (client_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_nc_email (invited_by_email),
    INDEX idx_nc_client (client_id),
    INDEX idx_nc_status (status)
) ENGINE=InnoDB;
