-- Fase 9D-2: cada nutri personaliza o PDF de prescricao com logo, cor e textos.
-- Todos os campos sao opcionais; o template default (marca Prandium) e usado quando ausentes.
ALTER TABLE users
    ADD COLUMN pdf_logo_base64    MEDIUMTEXT   NULL,   -- data URL PNG/JPG substitui o header default
    ADD COLUMN pdf_primary_color  VARCHAR(9)   NULL,   -- ex.: "#0e8f83" ou "#0e8f83ff"; senao usa teal default
    ADD COLUMN pdf_header_text    VARCHAR(500) NULL,   -- HTML sanitizado (Jsoup whitelist) exibido acima do titulo
    ADD COLUMN pdf_footer_text    VARCHAR(1000) NULL;  -- HTML sanitizado no rodape (endereco, telefone, redes)
