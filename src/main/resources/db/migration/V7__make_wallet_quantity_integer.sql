DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM wallet_holdings
        WHERE quantity <> trunc(quantity)
    ) THEN
        RAISE EXCEPTION 'wallet_holdings.quantity contains fractional values and cannot be converted to an integer quantity';
    END IF;
END $$;

ALTER TABLE wallet_holdings
    DROP CONSTRAINT chk_wallet_holdings_quantity_positive;

ALTER TABLE wallet_holdings
    ALTER COLUMN quantity TYPE NUMERIC(18, 0) USING trunc(quantity);

ALTER TABLE wallet_holdings
    ADD CONSTRAINT chk_wallet_holdings_quantity_positive CHECK (quantity > 0);
