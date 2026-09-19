-- V014: Fluxo de aprovacao de cardapio pelo nutricionista + notificacoes in-app/email.

ALTER TABLE menu_plans
    ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'APPROVED',
    ADD COLUMN submitted_to_nutri_id BIGINT NULL,
    ADD COLUMN submitted_at DATETIME NULL,
    ADD COLUMN reviewed_at DATETIME NULL,
    ADD COLUMN reviewed_by_id BIGINT NULL,
    ADD COLUMN review_note VARCHAR(2000) NULL;

CREATE INDEX idx_menuplan_pending ON menu_plans (submitted_to_nutri_id, status);

CREATE TABLE notifications (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    type VARCHAR(60) NOT NULL,           -- MENU_SUBMITTED, MENU_APPROVED, MENU_REJECTED, GENERIC
    title VARCHAR(255) NOT NULL,
    body VARCHAR(2000) NULL,
    entity_type VARCHAR(60) NULL,
    entity_id BIGINT NULL,
    read_at DATETIME NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_notif_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_notif_user_unread (user_id, read_at, created_at),
    INDEX idx_notif_user_created (user_id, created_at)
) ENGINE=InnoDB;
