-- Fase 9D-2-fix: personalizacao do PDF vai de users -> nutritionist_practices.
-- Motivo: um nutri pode ter varias practices (particular + vinculadas a empresas),
-- cada uma com identidade visual propria. A customizacao deve ser POR practice.

-- 1. Adiciona as 4 colunas em practices (mesma semantica de users).
ALTER TABLE nutritionist_practices
    ADD COLUMN pdf_logo_base64    MEDIUMTEXT   NULL,
    ADD COLUMN pdf_primary_color  VARCHAR(9)   NULL,
    ADD COLUMN pdf_header_text    VARCHAR(500) NULL,
    ADD COLUMN pdf_footer_text    VARCHAR(1000) NULL;

-- 2. Backfill: valores atuais em users vao pra practice particular do nutri
--    (a que tem company_id IS NULL). Se o nutri nao tiver particular por algum
--    motivo, o dado se perde — aceitavel pois o nutri pode reconfigurar.
UPDATE nutritionist_practices np
JOIN users u ON u.id = np.nutritionist_id
SET np.pdf_logo_base64   = u.pdf_logo_base64,
    np.pdf_primary_color = u.pdf_primary_color,
    np.pdf_header_text   = u.pdf_header_text,
    np.pdf_footer_text   = u.pdf_footer_text
WHERE np.company_id IS NULL
  AND (u.pdf_logo_base64 IS NOT NULL
       OR u.pdf_primary_color IS NOT NULL
       OR u.pdf_header_text IS NOT NULL
       OR u.pdf_footer_text IS NOT NULL);

-- 3. Remove as colunas de users. Assinatura (signature_base64) permanece em users
--    pois representa o CRN pessoal do profissional, valido em qualquer atendimento.
ALTER TABLE users
    DROP COLUMN pdf_logo_base64,
    DROP COLUMN pdf_primary_color,
    DROP COLUMN pdf_header_text,
    DROP COLUMN pdf_footer_text;
