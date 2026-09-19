-- V019: Backfill de sponsorship para vinculos ativos existentes (Fase 5C aplicada retroativamente).
-- Todo cliente com pelo menos 1 NCL ACTIVE deve estar em CLIENT_PLUS, com
-- sponsored_by_practice_id apontando para a practice do primeiro vinculo ativo.
--
-- Regras:
-- - Nao mexe em subscription paga propria (amount_cents > 0) — respeita quem paga sozinho.
-- - Nao mexe em subscription ja sponsored (ja processada).
-- - Apenas afeta subscriptions ACTIVE que estao em CLIENT_FREE.

UPDATE subscriptions s
JOIN (
    SELECT nc.client_id, MIN(nc.practice_id) AS practice_id
    FROM nutritionist_clients nc
    WHERE nc.status = 'ACTIVE'
      AND nc.practice_id IS NOT NULL
    GROUP BY nc.client_id
) picks ON picks.client_id = s.payer_id
SET s.plan_code = 'CLIENT_PLUS',
    s.sponsored_by_practice_id = picks.practice_id,
    s.amount_cents = 0
WHERE s.payer_type = 'USER'
  AND s.status = 'ACTIVE'
  AND s.plan_code = 'CLIENT_FREE'
  AND s.sponsored_by_practice_id IS NULL;
