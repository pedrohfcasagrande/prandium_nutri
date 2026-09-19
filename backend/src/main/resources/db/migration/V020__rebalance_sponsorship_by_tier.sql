-- V020: Fase 5G — sponsorship so concede CLIENT_PLUS quando o pagador tem tier PAGO.
-- Corrige o estado deixado por V019: reverte pra CLIENT_FREE os clientes cujo sponsor e
-- uma practice cujo pagador (empresa ou nutri) esta em tier=FREE.

UPDATE subscriptions s
JOIN nutritionist_practices np ON np.id = s.sponsored_by_practice_id
LEFT JOIN subscriptions payer
       ON (
            (np.company_id IS NOT NULL AND payer.payer_type = 'COMPANY'      AND payer.payer_id = np.company_id)
         OR (np.company_id IS NULL     AND payer.payer_type = 'NUTRITIONIST' AND payer.payer_id = np.nutritionist_id)
          )
      AND payer.status IN ('ACTIVE', 'TRIALING')
LEFT JOIN plans payer_plan ON payer_plan.code = payer.plan_code
SET s.plan_code = 'CLIENT_FREE',
    s.sponsored_by_practice_id = NULL,
    s.amount_cents = 0
WHERE s.payer_type = 'USER'
  AND s.status = 'ACTIVE'
  AND s.sponsored_by_practice_id IS NOT NULL
  AND (payer_plan.tier IS NULL OR payer_plan.tier = 'FREE');
