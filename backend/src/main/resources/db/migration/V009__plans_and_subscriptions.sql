-- V009: Catalogo de planos, precos multi-currency, assinaturas e uso.

CREATE TABLE plans (
    code VARCHAR(30) PRIMARY KEY,
    target VARCHAR(20) NOT NULL,        -- COMPANY, NUTRITIONIST, USER
    tier VARCHAR(30) NOT NULL,          -- FREE, STARTER, GROWTH, BUSINESS, PRO, PLUS
    name VARCHAR(100) NOT NULL,
    features JSON NOT NULL,
    active BIT(1) NOT NULL DEFAULT 1,
    sort_order INT NOT NULL DEFAULT 0
) ENGINE=InnoDB;

CREATE TABLE plan_prices (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    plan_code VARCHAR(30) NOT NULL,
    currency VARCHAR(3) NOT NULL,       -- BRL, USD, EUR
    amount_cents INT NOT NULL,
    provider VARCHAR(20) NOT NULL,      -- MERCADO_PAGO, STRIPE, MANUAL
    provider_price_id VARCHAR(255) NULL,
    active BIT(1) NOT NULL DEFAULT 1,
    CONSTRAINT fk_price_plan FOREIGN KEY (plan_code) REFERENCES plans(code) ON DELETE CASCADE,
    UNIQUE KEY uq_price (plan_code, currency)
) ENGINE=InnoDB;

CREATE TABLE subscriptions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    payer_type VARCHAR(20) NOT NULL,    -- COMPANY, NUTRITIONIST, USER
    payer_id BIGINT NOT NULL,           -- discriminado por payer_type; sem FK fisica
    plan_code VARCHAR(30) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    amount_cents INT NOT NULL,
    provider VARCHAR(20) NOT NULL,
    provider_subscription_id VARCHAR(255) NULL,
    status VARCHAR(30) NOT NULL,        -- TRIALING, ACTIVE, PAST_DUE, CANCELED, SUSPENDED
    current_period_start DATETIME NOT NULL,
    current_period_end DATETIME NOT NULL,
    cancel_at_period_end BIT(1) NOT NULL DEFAULT 0,
    canceled_at DATETIME NULL,
    started_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_sub_plan FOREIGN KEY (plan_code) REFERENCES plans(code),
    INDEX idx_sub_payer (payer_type, payer_id, status),
    INDEX idx_sub_status (status, current_period_end)
) ENGINE=InnoDB;

CREATE TABLE subscription_usage (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    subscription_id BIGINT NOT NULL,
    feature_key VARCHAR(60) NOT NULL,
    period_yyyymm VARCHAR(7) NOT NULL,  -- '2026-09'
    count INT NOT NULL DEFAULT 0,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uq_su (subscription_id, feature_key, period_yyyymm),
    CONSTRAINT fk_su_sub FOREIGN KEY (subscription_id) REFERENCES subscriptions(id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- Seed de planos ------------------------------------------------------------

INSERT INTO plans (code, target, tier, name, features, sort_order) VALUES
-- Empresa
('COMP_STARTER',  'COMPANY',      'STARTER',   'Starter',
    '{"maxNutritionists":3,"maxClientsPerNutri":30,"aiSuggestionsMonthly":null,"menuLimit":null}', 10),
('COMP_GROWTH',   'COMPANY',      'GROWTH',    'Growth',
    '{"maxNutritionists":10,"maxClientsPerNutri":50,"aiSuggestionsMonthly":null,"menuLimit":null}', 20),
('COMP_BUSINESS', 'COMPANY',      'BUSINESS',  'Business',
    '{"maxNutritionists":25,"maxClientsPerNutri":100,"aiSuggestionsMonthly":null,"menuLimit":null}', 30),

-- Nutricionista independente
('NUTRI_FREE',     'NUTRITIONIST', 'FREE',      'Free',
    '{"maxClients":3,"aiSuggestionsMonthly":10,"menuLimit":1}', 10),
('NUTRI_PRO',      'NUTRITIONIST', 'PRO',       'Pro',
    '{"maxClients":25,"aiSuggestionsMonthly":null,"menuLimit":null}', 20),
('NUTRI_BUSINESS', 'NUTRITIONIST', 'BUSINESS',  'Business',
    '{"maxClients":null,"aiSuggestionsMonthly":null,"menuLimit":null,"customBranding":true}', 30),

-- Cliente direto (B2C)
('CLIENT_FREE',   'USER',         'FREE',      'Free',
    '{"aiSuggestionsMonthly":3,"menuLimit":1}', 10),
('CLIENT_PLUS',   'USER',         'PLUS',      'Plus',
    '{"aiSuggestionsMonthly":null,"menuLimit":null}', 20);

-- Seed de precos (amounts em cents) ----------------------------------------

INSERT INTO plan_prices (plan_code, currency, amount_cents, provider) VALUES
-- Empresa
('COMP_STARTER',  'BRL',  49900, 'MERCADO_PAGO'),
('COMP_STARTER',  'USD',   9900, 'STRIPE'),
('COMP_STARTER',  'EUR',   8900, 'STRIPE'),
('COMP_GROWTH',   'BRL', 149900, 'MERCADO_PAGO'),
('COMP_GROWTH',   'USD',  29900, 'STRIPE'),
('COMP_GROWTH',   'EUR',  26900, 'STRIPE'),
('COMP_BUSINESS', 'BRL', 349900, 'MERCADO_PAGO'),
('COMP_BUSINESS', 'USD',  69900, 'STRIPE'),
('COMP_BUSINESS', 'EUR',  62900, 'STRIPE'),

-- Nutricionista
('NUTRI_FREE',     'BRL',      0, 'MANUAL'),
('NUTRI_FREE',     'USD',      0, 'MANUAL'),
('NUTRI_FREE',     'EUR',      0, 'MANUAL'),
('NUTRI_PRO',      'BRL',   7900, 'MERCADO_PAGO'),
('NUTRI_PRO',      'USD',   1900, 'STRIPE'),
('NUTRI_PRO',      'EUR',   1700, 'STRIPE'),
('NUTRI_BUSINESS', 'BRL',  14900, 'MERCADO_PAGO'),
('NUTRI_BUSINESS', 'USD',   3500, 'STRIPE'),
('NUTRI_BUSINESS', 'EUR',   3200, 'STRIPE'),

-- Cliente
('CLIENT_FREE',   'BRL',      0, 'MANUAL'),
('CLIENT_FREE',   'USD',      0, 'MANUAL'),
('CLIENT_FREE',   'EUR',      0, 'MANUAL'),
('CLIENT_PLUS',   'BRL',   1900, 'MERCADO_PAGO'),
('CLIENT_PLUS',   'USD',    500, 'STRIPE'),
('CLIENT_PLUS',   'EUR',    400, 'STRIPE');
