-- V005: Cardapios salvos (manuais ou gerados por IA)

CREATE TABLE menu_plans (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    name VARCHAR(255) NOT NULL,
    source VARCHAR(20) NOT NULL,
    content MEDIUMTEXT NOT NULL,
    start_date DATE NULL,
    end_date DATE NULL,
    active BIT(1) NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_menuplan_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_menuplan_user (user_id),
    INDEX idx_menuplan_active (user_id, active)
) ENGINE=InnoDB;
