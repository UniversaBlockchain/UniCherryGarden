CREATE TABLE ucp_transaction
(
    id              BIGINT                   NOT NULL GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    block_number    INTEGER REFERENCES ucp_block,
    txhash          CHAR(66)                 NOT NULL
        CHECK (ucp_is_valid_hex_hash(txhash, 66))
        UNIQUE,
    timestamp       TIMESTAMP WITH TIME ZONE NOT NULL,
    from_hash       CHAR(42)                 NOT NULL
        CHECK (ucp_is_valid_hex_hash(from_hash, 42)),
    to_hash         CHAR(42)                 NOT NULL
        CHECK (ucp_is_valid_hex_hash(to_hash, 42)),
    status          INT                      NULL, -- only since Byzantium
    is_status_ok    BOOLEAN                  NULL, -- only since Byzantium
    is_internal     BOOLEAN DEFAULT FALSE,
    ucp_comment     TEXT                     NULL,
    transaction_fee NUMERIC                  NOT NULL
        CHECK (transaction_fee >= 0),
    gas_price       NUMERIC                  NOT NULL
        CHECK (gas_price >= 0),
    gas_limit       BIGINT                   NOT NULL
        CHECK (gas_limit >= 0),
    gas_used        BIGINT                   NOT NULL
        CHECK (gas_used >= 0),
    nonce           INTEGER                  NOT NULL
        CHECK (nonce >= 0)
);
