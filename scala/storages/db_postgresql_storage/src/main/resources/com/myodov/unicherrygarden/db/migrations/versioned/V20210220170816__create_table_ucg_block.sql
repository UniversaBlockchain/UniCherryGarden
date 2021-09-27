CREATE TABLE ucg_block
(
    number      INTEGER                  NOT NULL PRIMARY KEY
        CHECK (number >= 0), -- is the block height
    hash        CHAR(66)                 NOT NULL
        CHECK (ucg_is_valid_hex_hash(hash, 66))
        UNIQUE,
    parent_hash CHAR(66)                 NOT NULL
        CHECK (ucg_is_valid_hex_hash(parent_hash, 66))
        UNIQUE
        REFERENCES ucg_block (hash)
            ON DELETE CASCADE,
    timestamp   TIMESTAMP WITH TIME ZONE NOT NULL
        UNIQUE,
    ucg_comment TEXT                     NULL
);
