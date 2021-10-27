ALTER TABLE ucg_transaction
    ADD COLUMN gas                 BIGINT NOT NULL
        CHECK ( gas >= 0 ),
    ADD COLUMN value               BIGINT NOT NULL
        CHECK ( value >= 0 ),
    ADD COLUMN effective_gas_price BIGINT NOT NULL
        CHECK ( effective_gas_price >= 0 ),
    ADD COLUMN cumulative_gas_used BIGINT NOT NULL
        CHECK ( cumulative_gas_used >= 0 ),
    DROP COLUMN gas_limit,
    DROP COLUMN timestamp,
    DROP COLUMN is_internal;
