CREATE TABLE IF NOT EXISTS company_ticker_catalog (
    company_name VARCHAR(255) NOT NULL,
    ticker_symbol VARCHAR(20) NOT NULL,
    exchange VARCHAR(50),
    CONSTRAINT pk_company_ticker_catalog PRIMARY KEY (ticker_symbol)
);

INSERT INTO company_ticker_catalog (company_name, ticker_symbol, exchange)
VALUES
    ('Apple Inc.', 'AAPL', 'NASDAQ'),
    ('Microsoft Corporation', 'MSFT', 'NASDAQ'),
    ('Alphabet Inc.', 'GOOGL', 'NASDAQ'),
    ('Amazon.com, Inc.', 'AMZN', 'NASDAQ'),
    ('Meta Platforms, Inc.', 'META', 'NASDAQ'),
    ('NVIDIA Corporation', 'NVDA', 'NASDAQ'),
    ('Tesla, Inc.', 'TSLA', 'NASDAQ'),
    ('Berkshire Hathaway Inc.', 'BRK.B', 'NYSE'),
    ('JPMorgan Chase & Co.', 'JPM', 'NYSE'),
    ('Johnson & Johnson', 'JNJ', 'NYSE')
ON CONFLICT (ticker_symbol) DO NOTHING;
