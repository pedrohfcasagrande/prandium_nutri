-- Fase 9B: dobras cutaneas (Jackson-Pollock) + perimetros faltantes

ALTER TABLE client_assessments
    -- Dobras cutaneas (mm) — protocolo Jackson-Pollock 7 pontos
    ADD COLUMN sf_tricipital       DOUBLE       NULL,
    ADD COLUMN sf_subscapular      DOUBLE       NULL,
    ADD COLUMN sf_suprailiac       DOUBLE       NULL,
    ADD COLUMN sf_abdominal        DOUBLE       NULL,
    ADD COLUMN sf_thigh            DOUBLE       NULL,
    ADD COLUMN sf_chest            DOUBLE       NULL,
    ADD COLUMN sf_midaxillary      DOUBLE       NULL,

    -- Protocolo declarado (JP_7, JP_3, DURNIN_4, POLLOCK_3, MANUAL...)
    ADD COLUMN skinfold_protocol   VARCHAR(30)  NULL,

    -- Perimetros faltantes (cm)
    ADD COLUMN thigh_cm            DOUBLE       NULL,
    ADD COLUMN calf_cm             DOUBLE       NULL,
    ADD COLUMN chest_cm            DOUBLE       NULL,
    ADD COLUMN forearm_cm          DOUBLE       NULL,
    ADD COLUMN abdomen_cm          DOUBLE       NULL;
