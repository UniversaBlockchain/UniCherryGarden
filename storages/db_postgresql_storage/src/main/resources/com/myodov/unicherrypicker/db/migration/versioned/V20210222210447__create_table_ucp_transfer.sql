CREATE TABLE ucp_transfer
(
    id          BIGINT   NOT NULL GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    currency_id INTEGER  NOT NULL REFERENCES ucp_currency,
    from_hash   CHAR(42) NOT NULL
        CHECK (ucp_is_valid_hex_hash(from_hash, 42)),
    to_hash     CHAR(42) NOT NULL
        CHECK (ucp_is_valid_hex_hash(to_hash, 42)),
    amount      NUMERIC  NOT NULL
        CHECK (amount >= 0),
    ucp_comment TEXT     NULL
);
