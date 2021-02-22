CREATE INDEX IF NOT EXISTS ucp_transaction_block_number
    ON ucp_transaction (block_number);

CREATE INDEX IF NOT EXISTS ucp_transaction_from_hash
    ON ucp_transaction (from_hash);

CREATE INDEX IF NOT EXISTS ucp_transaction_to_hash
    ON ucp_transaction (to_hash);

CREATE INDEX IF NOT EXISTS ucp_transaction_timestamp
    ON ucp_transaction (timestamp);
