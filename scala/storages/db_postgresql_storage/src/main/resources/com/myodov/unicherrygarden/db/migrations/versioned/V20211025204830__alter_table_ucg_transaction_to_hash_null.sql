-- to_hash will be null if it’s a transaction creating the smart contract
ALTER TABLE ucg_transaction
    ALTER COLUMN to_hash DROP NOT NULL,
    DROP CONSTRAINT ucg_transaction_to_hash_check,
    ADD CONSTRAINT ucg_transaction_to_hash_check
        CHECK (to_hash IS NULL OR ucg_is_valid_hex_hash((to_hash)::character varying, 42));
