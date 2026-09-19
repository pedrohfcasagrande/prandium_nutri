-- V007: Empresas + vinculo Company <-> Nutritionist
-- MVP: cada Company tem um unico owner (user com role=COMPANY).
-- Multi-admin fica para versao futura via tabela company_members.

CREATE TABLE companies (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    owner_user_id BIGINT NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    trade_name VARCHAR(255),
    tax_id VARCHAR(30),
    country_code VARCHAR(2) NOT NULL DEFAULT 'BR',
    contact_email VARCHAR(255) NOT NULL,
    contact_phone VARCHAR(30),
    active BIT(1) NOT NULL DEFAULT 1,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_company_owner FOREIGN KEY (owner_user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE company_nutritionists (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    company_id BIGINT NOT NULL,
    nutritionist_id BIGINT NULL,
    invited_by_email VARCHAR(255) NOT NULL,
    invite_token VARCHAR(255) NOT NULL UNIQUE,
    status VARCHAR(30) NOT NULL,
    invited_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    accepted_at DATETIME NULL,
    revoked_at DATETIME NULL,
    expires_at DATETIME NOT NULL,
    CONSTRAINT fk_cn_company FOREIGN KEY (company_id) REFERENCES companies(id) ON DELETE CASCADE,
    CONSTRAINT fk_cn_nutri FOREIGN KEY (nutritionist_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_cn_email (invited_by_email),
    INDEX idx_cn_nutri (nutritionist_id),
    INDEX idx_cn_company (company_id, status)
) ENGINE=InnoDB;
