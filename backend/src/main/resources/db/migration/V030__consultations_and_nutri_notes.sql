-- Fase 9E: prontuario + timeline.
-- 1) Notas privadas do nutri (nunca aparecem no PDF nem no MyAssessment).
ALTER TABLE client_assessments
    ADD COLUMN nutri_notes TEXT NULL;

-- 2) Registro de consultas: cada consulta e um evento formal na relacao nutri-cliente,
--    opcionalmente linkado a um snapshot criado no momento (retrato da ficha "como ela estava").
CREATE TABLE consultations (
    id                       BIGINT AUTO_INCREMENT PRIMARY KEY,
    client_id                BIGINT       NOT NULL,
    nutritionist_id          BIGINT       NOT NULL,
    practice_id              BIGINT       NULL,
    consultation_date        DATETIME     NOT NULL,
    type                     VARCHAR(20)  NOT NULL,   -- INITIAL / RETURN / EMERGENCY / CHECKUP
    chief_complaint          TEXT         NULL,       -- queixa/motivo da consulta
    findings                 TEXT         NULL,       -- achados/observacoes do nutri durante o atendimento
    plan                     TEXT         NULL,       -- conduta / plano terapeutico
    next_consultation_date   DATETIME     NULL,       -- proxima consulta agendada/recomendada
    snapshot_id              BIGINT       NULL,       -- snapshot da ficha no momento (nullable)
    created_at               DATETIME     NOT NULL,
    updated_at               DATETIME     NOT NULL,

    CONSTRAINT fk_cons_client   FOREIGN KEY (client_id)       REFERENCES users (id)                    ON DELETE CASCADE,
    CONSTRAINT fk_cons_nutri    FOREIGN KEY (nutritionist_id) REFERENCES users (id)                    ON DELETE CASCADE,
    CONSTRAINT fk_cons_practice FOREIGN KEY (practice_id)     REFERENCES nutritionist_practices (id)   ON DELETE SET NULL,
    CONSTRAINT fk_cons_snapshot FOREIGN KEY (snapshot_id)     REFERENCES assessment_snapshots (id)     ON DELETE SET NULL,

    INDEX idx_cons_client_date (client_id, consultation_date),
    INDEX idx_cons_nutri_date  (nutritionist_id, consultation_date)
);
