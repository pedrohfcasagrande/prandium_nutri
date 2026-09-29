-- Fase 9D-2 refino: trocar os textareas HTML (pdf_header_text/pdf_footer_text)
-- por campos estruturados. Nutricionistas nao precisam saber HTML pra montar o
-- cabecalho/rodape do proprio PDF.
--
-- Descartamos os dados antigos porque a feature e recente (nenhum ambiente prod
-- estabelecido) e o formato antigo era HTML — nao vale a pena escrever parser
-- pra recuperar migalhas.

ALTER TABLE nutritionist_practices
    DROP COLUMN pdf_header_text,
    DROP COLUMN pdf_footer_text;

ALTER TABLE nutritionist_practices
    -- Cabecalho: uma linha simples (nome do consultorio, CNPJ etc)
    ADD COLUMN pdf_header_line       VARCHAR(200) NULL,

    -- Rodape: 5 campos de contato + 1 nota livre
    ADD COLUMN pdf_footer_address    VARCHAR(300) NULL,
    ADD COLUMN pdf_footer_phone      VARCHAR(80)  NULL,
    ADD COLUMN pdf_footer_email      VARCHAR(150) NULL,
    ADD COLUMN pdf_footer_website    VARCHAR(200) NULL,
    ADD COLUMN pdf_footer_instagram  VARCHAR(80)  NULL,
    ADD COLUMN pdf_footer_note       VARCHAR(500) NULL;
