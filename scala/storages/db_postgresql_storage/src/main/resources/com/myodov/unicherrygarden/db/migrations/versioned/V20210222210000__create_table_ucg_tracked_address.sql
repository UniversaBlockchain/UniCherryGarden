CREATE TABLE ucg_tracked_address
(
    id                       BIGINT   NOT NULL GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    address                  CHAR(42) NOT NULL CHECK (ucg_is_valid_hex_hash(address, 42))
        UNIQUE,
    ucg_comment              TEXT     NULL,
    synced_from_block_number INTEGER  NOT NULL
        CHECK (synced_from_block_number >= 0),
    synced_to_block_number   INTEGER  NULL
        CHECK (synced_to_block_number IS NULL OR synced_to_block_number >= 0),
    CONSTRAINT "synced_to_block_number >= synced_from_block_number, if exists"
        CHECK (synced_to_block_number IS NULL OR synced_to_block_number >= synced_from_block_number)
);
