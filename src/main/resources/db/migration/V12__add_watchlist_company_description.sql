ALTER TABLE watchlist_entries
    ADD COLUMN IF NOT EXISTS company_description TEXT;
