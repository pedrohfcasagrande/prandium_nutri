-- Fase 10B: registro de hidratacao do cliente.
--
-- Rows sao pequenas (user_id + data + ml + timestamp). Nao guardamos tipo de
-- bebida — so a quantidade em ml. O total diario e derivado por agregacao
-- (SUM em WHERE user_id=? AND date=?). Progresso vs target_water_ml da
-- prescricao (fase 9C) fica no DashboardService.

CREATE TABLE water_intakes (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id     BIGINT      NOT NULL,
    date        DATE        NOT NULL,
    amount_ml   INT         NOT NULL,        -- positivo; front usa presets 200/300/500
    logged_at   DATETIME    NOT NULL,

    CONSTRAINT fk_water_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    INDEX idx_water_user_date (user_id, date)
);
