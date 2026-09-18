CREATE TABLE IF NOT EXISTS stock_projection_previews (
    id BIGSERIAL PRIMARY KEY,
    owner_id VARCHAR(100) NOT NULL,
    ticker_symbol VARCHAR(20) NOT NULL,
    content TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_stock_projection_previews_owner_ticker
    ON stock_projection_previews (owner_id, ticker_symbol, created_at DESC);
