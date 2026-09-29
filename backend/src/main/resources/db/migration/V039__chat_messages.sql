-- Fase 9G-1: chat cliente <-> nutri sobre um NutritionistClientLink ACTIVE.
--
-- Uma thread por link (par nutri+client). Autorizacao piggy-back:
-- se o link nao esta ACTIVE, ninguem pode ler/escrever ali.
--
-- Nao ha "conversation" separada — a chave e o link_id direto.
-- Simplifica: o link ja carrega a autorizacao (owner types + status).

CREATE TABLE chat_messages (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    link_id      BIGINT      NOT NULL,        -- FK nutritionist_clients(id)
    sender_id    BIGINT      NOT NULL,        -- FK users(id) — nutri OU client do link
    content      VARCHAR(2000) NOT NULL,
    created_at   DATETIME    NOT NULL,
    read_at      DATETIME    NULL,            -- lido pelo OUTRO lado do link

    CONSTRAINT fk_chat_link   FOREIGN KEY (link_id)   REFERENCES nutritionist_clients (id) ON DELETE CASCADE,
    CONSTRAINT fk_chat_sender FOREIGN KEY (sender_id) REFERENCES users (id)              ON DELETE CASCADE,

    INDEX idx_chat_link_created (link_id, created_at),
    INDEX idx_chat_link_read    (link_id, read_at)
);
