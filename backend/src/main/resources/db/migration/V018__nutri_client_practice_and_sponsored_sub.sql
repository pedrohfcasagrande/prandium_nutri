-- V018: Fase 5C — vinculo nutri-cliente aponta para uma practice especifica,
--                  e assinatura do cliente ganha `sponsored_by_practice_id` (auto-PLUS).

ALTER TABLE nutritionist_clients
    ADD COLUMN practice_id BIGINT NULL AFTER nutritionist_id,
    ADD CONSTRAINT fk_nc_practice FOREIGN KEY (practice_id)
        REFERENCES nutritionist_practices(id) ON DELETE SET NULL,
    ADD INDEX idx_nc_practice (practice_id);

-- Backfill: cada NCL existente aponta para a practice particular do nutri
-- (a heuristica mais segura: nao tinhamos conceito de "via empresa" ate hoje).
UPDATE nutritionist_clients nc
JOIN nutritionist_practices np
     ON np.nutritionist_id = nc.nutritionist_id AND np.company_id IS NULL
SET nc.practice_id = np.id
WHERE nc.practice_id IS NULL;

ALTER TABLE subscriptions
    ADD COLUMN sponsored_by_practice_id BIGINT NULL AFTER cancel_at_period_end,
    ADD CONSTRAINT fk_sub_sponsored_practice FOREIGN KEY (sponsored_by_practice_id)
        REFERENCES nutritionist_practices(id) ON DELETE SET NULL,
    ADD INDEX idx_sub_sponsored (sponsored_by_practice_id);
