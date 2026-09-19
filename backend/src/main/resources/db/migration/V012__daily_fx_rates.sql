-- V012: Tabela de cotacoes diarias para consolidar MRR em BRL.

CREATE TABLE daily_fx_rates (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    currency VARCHAR(3) NOT NULL,       -- USD, EUR, BRL
    rate_to_brl DECIMAL(12, 6) NOT NULL,
    rate_date DATE NOT NULL,
    source VARCHAR(30) NOT NULL DEFAULT 'MANUAL',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uq_fx_currency_day (currency, rate_date),
    INDEX idx_fx_currency (currency, rate_date)
) ENGINE=InnoDB;

-- Seed inicial (aproximado dez/2025). AdminMetricsService buscara a linha mais recente por moeda.
INSERT INTO daily_fx_rates (currency, rate_to_brl, rate_date, source) VALUES
('BRL', 1.000000, CURRENT_DATE, 'SEED'),
('USD', 5.500000, CURRENT_DATE, 'SEED'),
('EUR', 6.000000, CURRENT_DATE, 'SEED');
