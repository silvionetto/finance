ALTER TABLE stock_analyses
    ADD COLUMN IF NOT EXISTS analysis_type VARCHAR(20) NOT NULL DEFAULT 'ANALYSIS';

ALTER TABLE stock_analyses
    ADD CONSTRAINT chk_stock_analyses_type CHECK (analysis_type IN ('ANALYSIS', 'PREDICTION'));
