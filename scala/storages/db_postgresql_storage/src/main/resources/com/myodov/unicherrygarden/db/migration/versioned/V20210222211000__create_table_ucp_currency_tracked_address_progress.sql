CREATE TABLE ucp_currency_tracked_address_progress
(
    id                       BIGINT  NOT NULL GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    currency_id              INTEGER NOT NULL REFERENCES ucp_currency,
    tracked_address_id       BIGINT  NOT NULL REFERENCES ucp_tracked_address,
    synced_from_block_number INTEGER NOT NULL
        CHECK (synced_from_block_number >= 0),
    synced_to_block_number   INTEGER NULL
        CHECK (synced_to_block_number IS NULL OR synced_to_block_number >= 0),
    CONSTRAINT "synced_to_block_number >= synced_from_block_number, if exists"
        CHECK (synced_to_block_number IS NULL OR synced_to_block_number >= synced_from_block_number),
    CONSTRAINT "m2m_unique"
        UNIQUE (currency_id, tracked_address_id)
);


-- We don't need a separate index on (currency_id), it is a part of "m2m_unique" constraint
CREATE INDEX IF NOT EXISTS ucp_currency_tracked_address_tracked_address_id
    ON ucp_currency_tracked_address_progress (tracked_address_id);
