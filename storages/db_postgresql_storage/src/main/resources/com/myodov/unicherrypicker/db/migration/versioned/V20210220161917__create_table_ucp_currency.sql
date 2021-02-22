CREATE TABLE ucp_currency
(
    id           INTEGER           NOT NULL GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    type         UCP_CURRENCY_TYPE NOT NULL,
    dapp_address CHAR(42)          NULL CHECK (dapp_address IS NULL OR
                                               (LEFT(dapp_address, 2) = '0x' AND
                                                RIGHT(dapp_address, 40) = lower(RIGHT(dapp_address, 40))))
        UNIQUE,
    name         TEXT              NULL, -- optional as per ERC20
    symbol       TEXT              NULL, -- optional as per ERC20
    ucp_comment  TEXT              NULL,
    synced_from_block_number INTEGER NOT NULL
        CHECK (synced_from_block_number >= 0),
    synced_to_block_number INTEGER NULL
        CHECK (synced_to_block_number IS NULL OR synced_to_block_number >= 0),
    CONSTRAINT "dapp_address must be undefined for ETH and defined for ERC20"
        CHECK (CASE type
                   WHEN 'ETH' THEN
                       dapp_address IS NULL
                   WHEN 'ERC20' THEN
                       dapp_address IS NOT NULL
                   ELSE
                       FALSE
               END),
    CONSTRAINT "synced_to_block_number >= synced_from_block_number, if exists"
        CHECK (synced_to_block_number IS NULL OR synced_to_block_number >= synced_from_block_number)
);
