-- Fase 9C: prescricao energetica + macros na ficha do cliente

ALTER TABLE client_assessments
    -- Cabecalho da prescricao
    ADD COLUMN tmb_formula        VARCHAR(30)  NULL,   -- MIFFLIN / HARRIS_BENEDICT / KATCH_MCARDLE
    ADD COLUMN activity_factor    DOUBLE       NULL,   -- override manual; senao usa User.activityLevel

    -- Alvos calculados/manuais (source of truth do plano do cliente)
    ADD COLUMN target_calories    DOUBLE       NULL,   -- kcal/dia
    ADD COLUMN target_protein_g   DOUBLE       NULL,
    ADD COLUMN target_carbs_g     DOUBLE       NULL,
    ADD COLUMN target_fat_g       DOUBLE       NULL,
    ADD COLUMN target_water_ml    DOUBLE       NULL,   -- ml/dia

    -- Preferencias que dirigem o calculo (guardadas pro nutri revisar)
    ADD COLUMN protein_g_per_kg   DOUBLE       NULL,   -- ex.: 1.8
    ADD COLUMN fat_pct            DOUBLE       NULL,   -- ex.: 0.28 (28%)
    ADD COLUMN calorie_delta_pct  DOUBLE       NULL,   -- ex.: -0.20 (deficit 20%) / +0.15 (superavit 15%)

    ADD COLUMN prescribed_at      DATETIME     NULL;
