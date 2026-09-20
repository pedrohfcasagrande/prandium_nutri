CREATE TABLE refresh_tokens (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id      BIGINT       NOT NULL,
    token_hash   VARCHAR(128) NOT NULL UNIQUE,
    issued_at    DATETIME     NOT NULL,
    expires_at   DATETIME     NOT NULL,
    revoked_at   DATETIME     NULL,
    replaced_by  BIGINT       NULL,
    user_agent   VARCHAR(255) NULL,
    ip           VARCHAR(64)  NULL,
    CONSTRAINT fk_refresh_tokens_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_refresh_tokens_replaced FOREIGN KEY (replaced_by) REFERENCES refresh_tokens (id) ON DELETE SET NULL,
    INDEX idx_refresh_tokens_user (user_id),
    INDEX idx_refresh_tokens_expires (expires_at)
);
