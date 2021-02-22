CREATE INDEX IF NOT EXISTS ucp_transfer_currency_id
    ON ucp_transfer (currency_id);

CREATE INDEX IF NOT EXISTS ucp_transfer_from_hash
    ON ucp_transfer (from_hash);

CREATE INDEX IF NOT EXISTS ucp_transfer_to_hash
    ON ucp_transfer (to_hash);
