ALTER TABLE ucg_transaction
    ALTER COLUMN gas_price TYPE NUMERIC(78, 0),
    ALTER COLUMN value TYPE NUMERIC(78, 0),
    ALTER COLUMN effective_gas_price TYPE NUMERIC(78, 0);
