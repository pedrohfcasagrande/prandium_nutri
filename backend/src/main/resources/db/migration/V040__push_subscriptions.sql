-- Fase 9G-4: push notifications reais (Web Push API + VAPID).
--
-- Cada browser+device gera uma subscription (endpoint URL + chaves p256dh/auth
-- publicas para criptografia). Um user pode ter varias — desktop, celular,
-- outro browser. `endpoint` e unico globalmente (garantido pelo push service),
-- serve como chave natural para dedup.
--
-- Sem TTL — se o endpoint responde 404/410 na hora do send, o service deleta.

CREATE TABLE push_subscriptions (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id       BIGINT       NOT NULL,          -- FK users(id)
    endpoint      VARCHAR(500) NOT NULL,          -- URL do push service (FCM, Mozilla, Apple)
    p256dh        VARCHAR(255) NOT NULL,          -- chave publica para ECDH
    auth          VARCHAR(255) NOT NULL,          -- authenticator secret
    user_agent    VARCHAR(255) NULL,              -- so pra debug ("Chrome/desktop")
    created_at    DATETIME     NOT NULL,

    CONSTRAINT fk_push_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT uq_push_endpoint UNIQUE (endpoint),
    INDEX idx_push_user (user_id)
);
