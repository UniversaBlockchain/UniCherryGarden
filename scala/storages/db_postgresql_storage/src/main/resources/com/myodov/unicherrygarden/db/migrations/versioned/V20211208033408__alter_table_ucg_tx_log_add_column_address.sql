ALTER TABLE ucg_tx_log
    ADD COLUMN address char(42) NOT NULL
        CHECK (ucg_is_valid_hex_hash(address, 42));
