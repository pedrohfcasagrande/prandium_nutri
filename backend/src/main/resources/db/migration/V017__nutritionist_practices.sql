-- V017: Nutritionist practices (Fase 5A)
-- Cada nutri pode ter multiplos "perfis de trabalho":
--   - 1 particular (company_id NULL)
--   - N vinculados a empresas (company_id = <id>)
-- Bio/specialties/acceptsNewClients passam a viver por practice (nao mais por user).

CREATE TABLE nutritionist_practices (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    nutritionist_id BIGINT NOT NULL,
    company_id BIGINT NULL,
    bio VARCHAR(2000) NULL,
    specialties VARCHAR(500) NULL,
    accepts_new_clients BIT(1) NOT NULL DEFAULT 0,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',   -- ACTIVE, SUSPENDED
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_np_nutri FOREIGN KEY (nutritionist_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_np_company FOREIGN KEY (company_id) REFERENCES companies(id) ON DELETE CASCADE,
    -- Impede duplicar practice para o mesmo par (nutri, empresa).
    -- Colunas NULL em UNIQUE do MySQL sao consideradas distintas entre si, entao
    -- garantia de "so 1 particular por nutri" e feita via indice funcional abaixo.
    UNIQUE KEY uq_np_nutri_company (nutritionist_id, company_id),
    INDEX idx_np_nutri_status (nutritionist_id, status),
    INDEX idx_np_company_status (company_id, status)
) ENGINE=InnoDB;

-- Garante que so exista 1 practice particular (company_id NULL) por nutri.
-- MySQL 8+ suporta indice em expressao; usamos COALESCE para reduzir NULLs a 0.
CREATE UNIQUE INDEX uq_np_particular
    ON nutritionist_practices (nutritionist_id, (COALESCE(company_id, 0)));

-- Backfill: 1 practice particular para cada NUTRITIONIST existente,
-- copiando bio/specialties/accepts_new_clients do users.
INSERT INTO nutritionist_practices (nutritionist_id, company_id, bio, specialties, accepts_new_clients, status)
SELECT u.id, NULL, u.bio, u.specialties, u.accepts_new_clients, 'ACTIVE'
FROM users u
WHERE u.role = 'NUTRITIONIST';

-- Backfill: 1 practice vinculada para cada CompanyNutritionist ACTIVE.
-- Bio/specialties comecam vazios (nutri edita depois); accepts_new_clients=0 por default.
INSERT INTO nutritionist_practices (nutritionist_id, company_id, bio, specialties, accepts_new_clients, status)
SELECT cn.nutritionist_id, cn.company_id, NULL, NULL, 0, 'ACTIVE'
FROM company_nutritionists cn
WHERE cn.status = 'ACTIVE'
  AND cn.nutritionist_id IS NOT NULL;
