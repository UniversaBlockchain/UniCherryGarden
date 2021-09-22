CREATE TABLE ucp_transfer
(
    id          BIGINT   NOT NULL GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    currency_id INTEGER  NOT NULL REFERENCES ucp_currency,
    from_hash   CHAR(42) NULL
        CHECK (ucp_is_valid_hex_hash(from_hash, 42)),
    to_hash     CHAR(42) NULL
        CHECK (ucp_is_valid_hex_hash(to_hash, 42)),
    amount      NUMERIC  NOT NULL
        CHECK (amount >= 0),
    ucp_comment TEXT     NULL,
    CONSTRAINT cannot_have_both_sender_and_receiver_empty
        CHECK ( from_hash IS NOT NULL OR to_hash IS NOT NULL )
);

CREATE INDEX IF NOT EXISTS ucp_transfer_currency_id
    ON ucp_transfer (currency_id);

CREATE INDEX IF NOT EXISTS ucp_transfer_from_hash
    ON ucp_transfer (from_hash);

CREATE INDEX IF NOT EXISTS ucp_transfer_to_hash
    ON ucp_transfer (to_hash);
