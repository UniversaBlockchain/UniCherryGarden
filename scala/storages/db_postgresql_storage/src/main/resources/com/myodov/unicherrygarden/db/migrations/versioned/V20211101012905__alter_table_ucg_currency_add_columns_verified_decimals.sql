ALTER TABLE ucg_currency
    ADD COLUMN verified BOOLEAN  NOT NULL
        DEFAULT FALSE,
    ADD COLUMN decimals SMALLINT NULL
        CHECK (CASE type
                   WHEN 'ETH'::ucg_currency_type THEN
                       -- For ETH, decimals should be undefined as it is builtin
                       decimals IS NULL
                   WHEN 'ERC20'::ucg_currency_type THEN
                       -- For ERC20 tokens, decimal could be undefined (for non-verified tokens);
                       -- otherwise it should be between 0 and 79
                       decimals IS NULL OR decimals BETWEEN 0 AND 79
                   ELSE
                       -- New type added? Needs handling
                       FALSE
               END),
    ADD CONSTRAINT "Mandatory requirements for verified data"
        CHECK (CASE verified
                   WHEN FALSE THEN
                       -- No extra requirements for non-verified data
                       TRUE
                   ELSE
                       -- (WHEN verified = TRUE)
                       -- Here go the real mandatory requirements for verified data.
                       CASE type
                           WHEN 'ETH'::ucg_currency_type THEN
                               -- For ETH, no extra row-level requirements
                               TRUE
                           WHEN 'ERC20'::ucg_currency_type THEN
                               decimals IS NOT NULL
                           ELSE FALSE
                       END
               END);
