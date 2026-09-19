-- V003: Adiciona campos de saude no usuario e tabela de evolucoes

ALTER TABLE users
    ADD COLUMN medications VARCHAR(2000) NULL,
    ADD COLUMN has_bariatric_surgery BIT(1) NOT NULL DEFAULT 0,
    ADD COLUMN pre_existing_conditions VARCHAR(2000) NULL,
    ADD COLUMN allergies VARCHAR(2000) NULL;

CREATE TABLE progress_entries (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    date DATE NOT NULL,
    weight_kg DOUBLE NOT NULL,
    body_fat_percent DOUBLE NULL,
    waist_cm DOUBLE NULL,
    hip_cm DOUBLE NULL,
    arm_cm DOUBLE NULL,
    chest_cm DOUBLE NULL,
    note VARCHAR(1000) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_progress_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_progress_user_date (user_id, date)
) ENGINE=InnoDB;
