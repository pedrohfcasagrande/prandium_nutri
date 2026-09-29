-- Fase 9H-1: template do PDF passa a ser propriedade da empresa (ou da practice particular)
-- em vez de morar por practice. Assim todas as practices vinculadas a uma empresa herdam
-- o mesmo layout, mantendo consistencia visual da marca corporativa.

-- 1. Nova tabela pdf_templates. Um por dono (COMPANY.id ou PRACTICE.id particular).
CREATE TABLE pdf_templates (
    id                      BIGINT AUTO_INCREMENT PRIMARY KEY,
    owner_type              VARCHAR(20)  NOT NULL,   -- 'COMPANY' | 'PRACTICE'
    owner_id                BIGINT       NOT NULL,   -- FK logica (companies.id ou nutritionist_practices.id)

    pdf_logo_base64         MEDIUMTEXT   NULL,
    pdf_primary_color       VARCHAR(9)   NULL,
    pdf_header_line         VARCHAR(200) NULL,

    -- Rodape estruturado (mesmo modelo do V033; sera migrado pra template_fields na 9H-2)
    pdf_footer_address      VARCHAR(300) NULL,
    pdf_footer_phone        VARCHAR(80)  NULL,
    pdf_footer_email        VARCHAR(150) NULL,
    pdf_footer_website      VARCHAR(200) NULL,
    pdf_footer_instagram    VARCHAR(80)  NULL,
    pdf_footer_note         VARCHAR(500) NULL,

    created_at              DATETIME     NOT NULL,
    updated_at              DATETIME     NOT NULL,

    CONSTRAINT uq_pdf_tpl_owner UNIQUE (owner_type, owner_id)
);
CREATE INDEX idx_pdf_tpl_owner ON pdf_templates (owner_type, owner_id);

-- 2. Backfill: cada practice PARTICULAR (company_id NULL) com customizacao ganha
-- um pdf_templates row correspondente. Practices vinculadas nao migram — a empresa
-- comeca com template default (Prandium) e o admin configura via UI.
INSERT INTO pdf_templates (
    owner_type, owner_id,
    pdf_logo_base64, pdf_primary_color, pdf_header_line,
    pdf_footer_address, pdf_footer_phone, pdf_footer_email,
    pdf_footer_website, pdf_footer_instagram, pdf_footer_note,
    created_at, updated_at
)
SELECT 'PRACTICE', np.id,
       np.pdf_logo_base64, np.pdf_primary_color, np.pdf_header_line,
       np.pdf_footer_address, np.pdf_footer_phone, np.pdf_footer_email,
       np.pdf_footer_website, np.pdf_footer_instagram, np.pdf_footer_note,
       NOW(), NOW()
FROM nutritionist_practices np
WHERE np.company_id IS NULL
  AND (np.pdf_logo_base64 IS NOT NULL
    OR np.pdf_primary_color IS NOT NULL
    OR np.pdf_header_line IS NOT NULL
    OR np.pdf_footer_address IS NOT NULL
    OR np.pdf_footer_phone IS NOT NULL
    OR np.pdf_footer_email IS NOT NULL
    OR np.pdf_footer_website IS NOT NULL
    OR np.pdf_footer_instagram IS NOT NULL
    OR np.pdf_footer_note IS NOT NULL);

-- 3. Drop das colunas de PDF em nutritionist_practices (movidas pra pdf_templates).
ALTER TABLE nutritionist_practices
    DROP COLUMN pdf_logo_base64,
    DROP COLUMN pdf_primary_color,
    DROP COLUMN pdf_header_line,
    DROP COLUMN pdf_footer_address,
    DROP COLUMN pdf_footer_phone,
    DROP COLUMN pdf_footer_email,
    DROP COLUMN pdf_footer_website,
    DROP COLUMN pdf_footer_instagram,
    DROP COLUMN pdf_footer_note;
