ALTER TABLE ucg_transaction
    ADD COLUMN transaction_index INTEGER NOT NULL
        CHECK (transaction_index >= 0),
    ADD CONSTRAINT "transaction index unique in block"
        UNIQUE (block_number, transaction_index);
