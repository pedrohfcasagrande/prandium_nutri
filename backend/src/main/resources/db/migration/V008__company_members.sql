-- V008: Multi-admin da empresa + fluxo sales-led (empresa criada por admin, owner aceita depois)

-- Empresa pode ser criada pelo admin da plataforma sem owner ainda (owner aceita convite depois)
ALTER TABLE companies MODIFY COLUMN owner_user_id BIGINT NULL;
ALTER TABLE companies ADD COLUMN created_by_admin_id BIGINT NULL;
ALTER TABLE companies ADD CONSTRAINT fk_company_created_by
    FOREIGN KEY (created_by_admin_id) REFERENCES users(id);

-- Membros da empresa (owner + admins adicionais).
-- Novos admins tambem passam por convite similar aos nutris.
CREATE TABLE company_members (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    company_id BIGINT NOT NULL,
    user_id BIGINT NULL,                        -- null enquanto convite pendente
    invited_by_email VARCHAR(255) NOT NULL,
    invite_token VARCHAR(255) NOT NULL UNIQUE,
    role_in_company VARCHAR(20) NOT NULL,       -- OWNER, ADMIN
    status VARCHAR(30) NOT NULL,                -- PENDING, ACTIVE, REJECTED, REVOKED_BY_COMPANY, EXPIRED
    invited_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    accepted_at DATETIME NULL,
    revoked_at DATETIME NULL,
    expires_at DATETIME NOT NULL,
    CONSTRAINT fk_cm_company FOREIGN KEY (company_id) REFERENCES companies(id) ON DELETE CASCADE,
    CONSTRAINT fk_cm_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    UNIQUE KEY uq_cm_user (company_id, user_id),
    INDEX idx_cm_email (invited_by_email),
    INDEX idx_cm_status (status)
) ENGINE=InnoDB;
