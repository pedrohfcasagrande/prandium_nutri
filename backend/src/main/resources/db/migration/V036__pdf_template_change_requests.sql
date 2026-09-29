-- Fase 9H-3: nutri vinculado (com template PDF readOnly herdado da empresa)
-- pode SUGERIR mudancas via change_requests. Um admin da empresa (qualquer
-- CompanyMember) aprova ou rejeita. Aprovacao aplica o estado proposto.
--
-- Design: guardamos o ESTADO COMPLETO proposto em vez de patch/diff.
-- Motivos:
--   1) Facil renderizar "antes vs depois" na UI.
--   2) Aplicar = overwrite, sem risco de conflito com edicoes paralelas.
--   3) Trade-off aceito: se o template muda entre o pedido e a aprovacao,
--      o admin ve as duas versoes e decide.

CREATE TABLE pdf_template_change_requests (
    id                      BIGINT AUTO_INCREMENT PRIMARY KEY,
    template_id             BIGINT       NOT NULL,
    requested_by            BIGINT       NOT NULL,      -- FK users(id)
    status                  VARCHAR(20)  NOT NULL,      -- PENDING | APPROVED | REJECTED

    -- Estado proposto (mesmo shape do template atual)
    proposed_header_line    VARCHAR(200) NULL,
    proposed_primary_color  VARCHAR(9)   NULL,
    proposed_logo_base64    MEDIUMTEXT   NULL,
    proposed_fields         JSON         NOT NULL,       -- lista serializada de fields

    note                    VARCHAR(1000) NULL,          -- msg do nutri explicando o motivo
    reviewed_by             BIGINT       NULL,           -- FK users(id) — admin que decidiu
    reviewed_at             DATETIME     NULL,
    review_note             VARCHAR(1000) NULL,          -- motivo da rejeicao (opcional na aprovacao)

    created_at              DATETIME     NOT NULL,
    updated_at              DATETIME     NOT NULL,

    CONSTRAINT fk_pdf_req_tpl        FOREIGN KEY (template_id) REFERENCES pdf_templates (id) ON DELETE CASCADE,
    CONSTRAINT fk_pdf_req_requester  FOREIGN KEY (requested_by) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_pdf_req_reviewer   FOREIGN KEY (reviewed_by) REFERENCES users (id) ON DELETE SET NULL,

    INDEX idx_pdf_req_tpl_status (template_id, status, created_at),
    INDEX idx_pdf_req_requester  (requested_by, created_at)
);
