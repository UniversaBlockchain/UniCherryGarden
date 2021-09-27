CREATE TABLE ucg_currency
(
    id                     INTEGER           NOT NULL GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    type                   UCG_CURRENCY_TYPE NOT NULL,
    dapp_address           CHAR(42)          NULL CHECK (dapp_address IS NULL OR
                                                         ucg_is_valid_hex_hash(dapp_address, 42))
        UNIQUE,
    name                   TEXT              NULL, -- optional as per ERC20
    symbol                 TEXT              NULL, -- optional as per ERC20
    ucg_comment            TEXT              NULL,
    sync_from_block_number INTEGER           NOT NULL
        CHECK (sync_from_block_number >= 0),
    CONSTRAINT "dapp_address must be undefined for ETH and defined for ERC20"
        CHECK (CASE TYPE
                   WHEN 'ETH' THEN
                       dapp_address IS NULL
                   WHEN 'ERC20' THEN
                       dapp_address IS NOT NULL
                   ELSE
                       FALSE
               END)
);
