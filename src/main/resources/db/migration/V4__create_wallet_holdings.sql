CREATE TABLE IF NOT EXISTS wallet_holdings (
    owner_id VARCHAR(100) NOT NULL,
    symbol VARCHAR(20) NOT NULL,
    company_name VARCHAR(255) NOT NULL,
    quantity NUMERIC(18, 6) NOT NULL,
    average_cost NUMERIC(19, 4) NOT NULL,
    currency_code VARCHAR(3) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_wallet_holdings PRIMARY KEY (owner_id, symbol),
    CONSTRAINT chk_wallet_holdings_quantity_positive CHECK (quantity > 0),
    CONSTRAINT chk_wallet_holdings_average_cost_non_negative CHECK (average_cost >= 0),
    CONSTRAINT chk_wallet_holdings_currency_code_length CHECK (char_length(currency_code) = 3)
);

CREATE INDEX IF NOT EXISTS idx_wallet_holdings_owner_updated_at
    ON wallet_holdings (owner_id, updated_at DESC);
