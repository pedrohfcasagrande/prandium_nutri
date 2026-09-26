-- Fase 9A: exames laboratoriais + snapshots historicos da ficha

-- 1. Adiciona colunas de exames laboratoriais em client_assessments.
-- Todas nullable — nutri preenche o que for relevante para o caso.
ALTER TABLE client_assessments
    ADD COLUMN lab_date            DATE         NULL,
    ADD COLUMN glucose_fasting     DOUBLE       NULL,
    ADD COLUMN hba1c               DOUBLE       NULL,
    ADD COLUMN insulin_fasting     DOUBLE       NULL,
    ADD COLUMN total_cholesterol   DOUBLE       NULL,
    ADD COLUMN ldl                 DOUBLE       NULL,
    ADD COLUMN hdl                 DOUBLE       NULL,
    ADD COLUMN triglycerides       DOUBLE       NULL,
    ADD COLUMN tsh                 DOUBLE       NULL,
    ADD COLUMN t4_free             DOUBLE       NULL,
    ADD COLUMN vitamin_d           DOUBLE       NULL,
    ADD COLUMN vitamin_b12         DOUBLE       NULL,
    ADD COLUMN iron                DOUBLE       NULL,
    ADD COLUMN ferritin            DOUBLE       NULL,
    ADD COLUMN crp                 DOUBLE       NULL,
    ADD COLUMN urea                DOUBLE       NULL,
    ADD COLUMN creatinine          DOUBLE       NULL,
    ADD COLUMN uric_acid           DOUBLE       NULL,
    ADD COLUMN lab_notes           TEXT         NULL;

-- 2. Snapshots imutaveis da ficha, criados manualmente pelo nutri ao clicar
--    "Nova consulta". A ficha corrente continua sendo UPSERT normal; o snapshot
--    preserva o estado no momento do clique para timeline/historico.
--    Payload em JSON pra permitir schema evoluir sem migrations.
CREATE TABLE assessment_snapshots (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    assessment_id  BIGINT       NOT NULL,
    version        INT          NOT NULL,
    label          VARCHAR(255) NULL,
    payload        JSON         NOT NULL,
    created_by     BIGINT       NOT NULL,
    created_at     DATETIME     NOT NULL,

    CONSTRAINT fk_snapshot_assessment FOREIGN KEY (assessment_id) REFERENCES client_assessments (id) ON DELETE CASCADE,
    CONSTRAINT fk_snapshot_created_by FOREIGN KEY (created_by)    REFERENCES users (id)              ON DELETE RESTRICT,

    CONSTRAINT uq_snapshot_assessment_version UNIQUE (assessment_id, version),
    INDEX idx_snapshot_assessment (assessment_id, created_at)
);
