-- Fase 9H-2: campos modulares no rodape do PDF.
-- Ate agora os 6 campos de contato (address, phone, email, website, instagram, note)
-- eram colunas fixas. Migramos pra uma tabela filha onde cada linha e um campo com
-- label, tipo e valor — o dono do template escolhe quais campos existem, o nome e a ordem.

CREATE TABLE pdf_template_fields (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    template_id         BIGINT       NOT NULL,
    label               VARCHAR(100) NOT NULL,      -- rotulo interno (nao aparece no PDF)
    field_type          VARCHAR(20)  NOT NULL,      -- TEXT | TEXTAREA | URL | EMAIL | TEL | INSTAGRAM
    field_value         VARCHAR(1000) NULL,          -- 'value' e reservado em H2; usar prefixo
    position            INT          NOT NULL,      -- 0-based; ordem de renderizacao
    created_at          DATETIME     NOT NULL,
    updated_at          DATETIME     NOT NULL,

    CONSTRAINT fk_pdf_field_tpl FOREIGN KEY (template_id) REFERENCES pdf_templates (id) ON DELETE CASCADE,
    CONSTRAINT uq_pdf_field_pos UNIQUE (template_id, position),
    INDEX idx_pdf_field_tpl (template_id, position)
);

-- Backfill: cada template com colunas footer preenchidas vira N rows.
-- Position segue a ordem visual atual (address, phone, email, website, instagram, note).
INSERT INTO pdf_template_fields (template_id, label, field_type, field_value, position, created_at, updated_at)
SELECT id, 'Endereço', 'TEXT', pdf_footer_address, 0, NOW(), NOW()
FROM pdf_templates WHERE pdf_footer_address IS NOT NULL AND pdf_footer_address <> '';

INSERT INTO pdf_template_fields (template_id, label, field_type, field_value, position, created_at, updated_at)
SELECT id, 'Telefone', 'TEL', pdf_footer_phone, 1, NOW(), NOW()
FROM pdf_templates WHERE pdf_footer_phone IS NOT NULL AND pdf_footer_phone <> '';

INSERT INTO pdf_template_fields (template_id, label, field_type, field_value, position, created_at, updated_at)
SELECT id, 'E-mail', 'EMAIL', pdf_footer_email, 2, NOW(), NOW()
FROM pdf_templates WHERE pdf_footer_email IS NOT NULL AND pdf_footer_email <> '';

INSERT INTO pdf_template_fields (template_id, label, field_type, field_value, position, created_at, updated_at)
SELECT id, 'Site', 'URL', pdf_footer_website, 3, NOW(), NOW()
FROM pdf_templates WHERE pdf_footer_website IS NOT NULL AND pdf_footer_website <> '';

INSERT INTO pdf_template_fields (template_id, label, field_type, field_value, position, created_at, updated_at)
SELECT id, 'Instagram', 'INSTAGRAM', pdf_footer_instagram, 4, NOW(), NOW()
FROM pdf_templates WHERE pdf_footer_instagram IS NOT NULL AND pdf_footer_instagram <> '';

INSERT INTO pdf_template_fields (template_id, label, field_type, field_value, position, created_at, updated_at)
SELECT id, 'Observações', 'TEXTAREA', pdf_footer_note, 5, NOW(), NOW()
FROM pdf_templates WHERE pdf_footer_note IS NOT NULL AND pdf_footer_note <> '';

ALTER TABLE pdf_templates
    DROP COLUMN pdf_footer_address,
    DROP COLUMN pdf_footer_phone,
    DROP COLUMN pdf_footer_email,
    DROP COLUMN pdf_footer_website,
    DROP COLUMN pdf_footer_instagram,
    DROP COLUMN pdf_footer_note;
