INSERT INTO company_ticker_catalog (company_name, ticker_symbol, exchange)
VALUES
    ('ABN AMRO Bank N.V.', 'ABN.AS', 'AMS'),
    ('ASML Holding N.V.', 'ASML.AS', 'AMS')
ON CONFLICT (ticker_symbol) DO UPDATE
SET company_name = EXCLUDED.company_name,
    exchange = EXCLUDED.exchange;
