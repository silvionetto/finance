CREATE TABLE IF NOT EXISTS watchlist_entries (
    owner_id VARCHAR(100) NOT NULL,
    symbol VARCHAR(20) NOT NULL,
    company_name VARCHAR(255),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_watchlist_entries PRIMARY KEY (owner_id, symbol)
);
