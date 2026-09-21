CREATE TABLE client_assessments (
    id                        BIGINT AUTO_INCREMENT PRIMARY KEY,
    client_id                 BIGINT       NOT NULL,
    nutritionist_id           BIGINT       NOT NULL,
    practice_id               BIGINT       NULL,
    assessment_date           DATETIME     NOT NULL,

    -- Antropometria
    weight_kg                 DOUBLE       NULL,
    height_cm                 INT          NULL,
    body_fat_pct              DOUBLE       NULL,
    waist_cm                  DOUBLE       NULL,
    hip_cm                    DOUBLE       NULL,
    neck_cm                   DOUBLE       NULL,
    arm_cm                    DOUBLE       NULL,

    -- Perfil clinico
    chief_complaint           TEXT         NULL,
    medical_history           TEXT         NULL,
    medications               TEXT         NULL,
    pre_existing_conditions   TEXT         NULL,
    allergies                 TEXT         NULL,
    family_history            TEXT         NULL,

    -- Habitos alimentares
    dietary_recall_24h        TEXT         NULL,
    meal_frequency            VARCHAR(500) NULL,
    water_intake_liters       DOUBLE       NULL,
    alcohol_frequency         VARCHAR(500) NULL,
    food_preferences          TEXT         NULL,
    food_aversions            TEXT         NULL,

    -- Estilo de vida
    physical_activity_details TEXT         NULL,
    sleep_hours               DOUBLE       NULL,
    stress_level              VARCHAR(20)  NULL,
    smoker                    BOOLEAN      NOT NULL DEFAULT FALSE,
    smoking_details           VARCHAR(500) NULL,

    -- Objetivos
    goal_notes                TEXT         NULL,
    target_weight_kg          DOUBLE       NULL,
    target_date               DATE         NULL,

    -- Meta
    notes                     TEXT         NULL,
    created_at                DATETIME     NOT NULL,
    updated_at                DATETIME     NOT NULL,

    CONSTRAINT fk_assessment_client  FOREIGN KEY (client_id)       REFERENCES users (id)                    ON DELETE CASCADE,
    CONSTRAINT fk_assessment_nutri   FOREIGN KEY (nutritionist_id) REFERENCES users (id)                    ON DELETE CASCADE,
    CONSTRAINT fk_assessment_pract   FOREIGN KEY (practice_id)     REFERENCES nutritionist_practices (id)   ON DELETE SET NULL,

    -- Uma unica ficha por par (nutri, cliente) — edita ao inves de duplicar
    CONSTRAINT uq_assessment_nutri_client UNIQUE (nutritionist_id, client_id),

    INDEX idx_assessment_client (client_id),
    INDEX idx_assessment_practice (practice_id)
);
