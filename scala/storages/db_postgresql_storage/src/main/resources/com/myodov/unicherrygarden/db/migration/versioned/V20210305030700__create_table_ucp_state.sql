CREATE TABLE ucp_state
(
    restarted_at             TIMESTAMP WITH TIME ZONE NOT NULL,
    last_heartbeat_at        TIMESTAMP WITH TIME ZONE NOT NULL,
    sync_state               TEXT                     NOT NULL,
    synced_from_block_number INTEGER                  NULL
        CHECK (synced_from_block_number IS NULL OR synced_from_block_number >= 0),
    synced_to_block_number   INTEGER                  NULL
        CHECK (synced_to_block_number IS NULL OR synced_to_block_number >= 0),
    CONSTRAINT "synced_to_block_number >= synced_from_block_number, if exists"
        CHECK (synced_to_block_number IS NULL OR synced_to_block_number >= synced_from_block_number)
);

-- Ensures that the ucp_state table can have only one row.
CREATE UNIQUE INDEX ucp_state_one_row
    ON ucp_state ((TRUE));
