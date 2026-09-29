-- Fase 9D-3: log de emissao do PDF de prescricao com hash SHA-256.
--
-- Cada render bem-sucedido do PDF cria uma row aqui. O hash SHA-256 dos bytes
-- permite verificacao publica de autenticidade — dado um PDF questionado, o
-- hash e comparado com registros para confirmar "esse documento foi emitido
-- em X data pelo nutri Y". O snapshot_id (opcional) linka o estado da ficha
-- no momento da emissao pra reconstituir historicamente.
--
-- Nao armazenamos o PDF em si (evita explosao de storage) — apenas o hash e
-- metadados. Se o usuario quiser reobter o PDF, gera de novo a partir da ficha
-- ou snapshot.

CREATE TABLE prescription_issuances (
    id                   BIGINT AUTO_INCREMENT PRIMARY KEY,
    assessment_id        BIGINT       NOT NULL,        -- FK client_assessments(id)
    snapshot_id          BIGINT       NULL,            -- FK assessment_snapshots(id), opcional
    nutri_id             BIGINT       NOT NULL,        -- FK users(id) — dono da ficha
    client_id            BIGINT       NOT NULL,        -- FK users(id) — paciente

    requester_role       VARCHAR(20)  NOT NULL,        -- 'NUTRITIONIST' | 'CLIENT'
    pdf_sha256           CHAR(64)     NOT NULL,        -- hash hex minusculo
    pdf_size_bytes       BIGINT       NOT NULL,

    issued_at            DATETIME     NOT NULL,

    CONSTRAINT fk_issuance_assessment FOREIGN KEY (assessment_id) REFERENCES client_assessments (id) ON DELETE CASCADE,
    CONSTRAINT fk_issuance_snapshot   FOREIGN KEY (snapshot_id)   REFERENCES assessment_snapshots (id) ON DELETE SET NULL,
    CONSTRAINT fk_issuance_nutri      FOREIGN KEY (nutri_id)      REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_issuance_client     FOREIGN KEY (client_id)     REFERENCES users (id) ON DELETE CASCADE,

    INDEX idx_issuance_assessment (assessment_id, issued_at),
    INDEX idx_issuance_client     (client_id, issued_at),
    INDEX idx_issuance_hash       (pdf_sha256)
);
