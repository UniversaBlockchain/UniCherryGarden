CREATE TABLE ucg_planted_transfer
(
    id                BIGINT                   NOT NULL GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    added_at          TIMESTAMP WITH TIME ZONE NOT NULL
        DEFAULT now(),
    modified_at       TIMESTAMP WITH TIME ZONE NOT NULL
        DEFAULT now(),
    broadcasted_at    TIMESTAMP WITH TIME ZONE NOT NULL
        DEFAULT now(),
    next_broadcast_at TIMESTAMP WITH TIME ZONE NULL
        DEFAULT now() + '5 minutes',
    sender            CHAR(42)                 NOT NULL
        CHECK (ucg_is_valid_hex_hash(sender, 42)),
    receiver          CHAR(42)                 NOT NULL
        CHECK (ucg_is_valid_hex_hash(sender, 42)),
    currency_id       INTEGER                  NOT NULL
        REFERENCES ucg_currency (id)
            ON DELETE CASCADE,
    amount            NUMERIC(158, 79)         NOT NULL
        CHECK (amount >= 0),
    data              BYTEA                    NOT NULL,
    chain_id          INTEGER                  NOT NULL
        CHECK (chain_id = -1 OR chain_id >= 1),
    nonce             INTEGER                  NOT NULL
        CHECK (nonce >= 0),
    gas_limit         NUMERIC(78, 0)           NOT NULL
        CHECK (gas_limit >= 0),
    max_priority_fee  NUMERIC(158, 79)         NOT NULL
        CHECK (max_priority_fee >= 0),
    max_fee           NUMERIC(158, 79)         NOT NULL
        CHECK (max_fee >= 0),
    ucg_comment       TEXT                     NULL,
    error             TEXT                     NULL
);

CREATE INDEX IF NOT EXISTS ucg_planted_transfer_added_at
    ON ucg_planted_transfer (added_at);

CREATE INDEX IF NOT EXISTS ucg_planted_transfer_modified_at
    ON ucg_planted_transfer (modified_at);

CREATE INDEX IF NOT EXISTS ucg_planted_transfer_broadcasted_at
    ON ucg_planted_transfer (broadcasted_at);

CREATE INDEX IF NOT EXISTS ucg_planted_transfer_next_broadcast_at
    ON ucg_planted_transfer (next_broadcast_at);

CREATE INDEX IF NOT EXISTS ucg_planted_transfer_sender_nonce
    ON ucg_planted_transfer (sender, nonce);

CREATE INDEX IF NOT EXISTS ucg_planted_transfer_currency_id
    ON ucg_planted_transfer (currency_id);
