CREATE TABLE IF NOT EXISTS stock_analyses (
    id BIGSERIAL PRIMARY KEY,
    owner_id VARCHAR(100) NOT NULL,
    symbol VARCHAR(20) NOT NULL,
    company_name VARCHAR(255),
    generated_output TEXT NOT NULL,
    baseline_price NUMERIC(19, 6),
    baseline_change NUMERIC(19, 6),
    baseline_change_percent NUMERIC(19, 6),
    baseline_currency_code VARCHAR(3),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_stock_analyses_owner_symbol_created
    ON stock_analyses (owner_id, symbol, created_at DESC);

CREATE TABLE IF NOT EXISTS market_observations (
    id BIGSERIAL PRIMARY KEY,
    owner_id VARCHAR(100) NOT NULL,
    symbol VARCHAR(20) NOT NULL,
    price NUMERIC(19, 6),
    change_value NUMERIC(19, 6),
    change_percent NUMERIC(19, 6),
    open_price NUMERIC(19, 6),
    close_price NUMERIC(19, 6),
    high_price NUMERIC(19, 6),
    low_price NUMERIC(19, 6),
    volume NUMERIC(24, 6),
    currency_code VARCHAR(3),
    provider VARCHAR(50),
    observed_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_market_observations_owner_symbol_observed
    ON market_observations (owner_id, symbol, observed_at);
