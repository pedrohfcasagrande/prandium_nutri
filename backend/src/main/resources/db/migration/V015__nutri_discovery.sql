-- V015: Discovery de nutricionistas + pedidos de vinculo iniciados pelo cliente.

ALTER TABLE users
    ADD COLUMN accepts_new_clients BIT(1) NOT NULL DEFAULT 0,
    ADD COLUMN bio VARCHAR(2000) NULL,
    ADD COLUMN specialties VARCHAR(500) NULL;

CREATE INDEX idx_users_accepts_new ON users (role, accepts_new_clients, active);

ALTER TABLE nutritionist_clients
    ADD COLUMN initiated_by_client BIT(1) NOT NULL DEFAULT 0,
    ADD COLUMN request_message VARCHAR(1000) NULL,
    ADD COLUMN reject_note VARCHAR(1000) NULL;
