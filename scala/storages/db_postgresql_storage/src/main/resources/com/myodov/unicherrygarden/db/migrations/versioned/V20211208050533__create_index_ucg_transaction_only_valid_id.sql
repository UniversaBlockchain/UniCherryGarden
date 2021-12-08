CREATE UNIQUE INDEX IF NOT EXISTS ucg_transaction_only_valid_id
    ON ucg_transaction (id)
    WHERE status IS NULL OR status = 1;
