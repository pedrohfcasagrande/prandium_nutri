-- V001: Schema inicial (users, foods, meals, meal_items)

CREATE TABLE users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    password_hash VARCHAR(255) NULL,
    role VARCHAR(20) NOT NULL,
    provider VARCHAR(20) NOT NULL,
    provider_id VARCHAR(255) NULL,
    profile_completed BIT(1) NOT NULL DEFAULT 0,
    active BIT(1) NOT NULL DEFAULT 1,
    consent_accepted_at DATETIME NULL,
    sex VARCHAR(10) NULL,
    birth_date DATE NULL,
    weight_kg DOUBLE NULL,
    height_cm DOUBLE NULL,
    activity_level VARCHAR(20) NULL,
    goal VARCHAR(20) NULL,
    preferences VARCHAR(2000) NULL
) ENGINE=InnoDB;

CREATE TABLE foods (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    category VARCHAR(100) NULL,
    calories_per100g DOUBLE NOT NULL,
    protein_per100g DOUBLE NOT NULL,
    carbs_per100g DOUBLE NOT NULL,
    fat_per100g DOUBLE NOT NULL,
    INDEX idx_food_name (name)
) ENGINE=InnoDB;

CREATE TABLE meals (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    date DATE NOT NULL,
    time TIME NULL,
    type VARCHAR(20) NOT NULL,
    CONSTRAINT fk_meal_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_meal_user_date (user_id, date)
) ENGINE=InnoDB;

CREATE TABLE meal_items (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    meal_id BIGINT NOT NULL,
    food_id BIGINT NOT NULL,
    grams DOUBLE NOT NULL,
    CONSTRAINT fk_item_meal FOREIGN KEY (meal_id) REFERENCES meals(id) ON DELETE CASCADE,
    CONSTRAINT fk_item_food FOREIGN KEY (food_id) REFERENCES foods(id)
) ENGINE=InnoDB;
