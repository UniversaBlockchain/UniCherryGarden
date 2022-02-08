ALTER TABLE ucg_currency
    ADD COLUMN transfer_gas_limit BIGINT
        DEFAULT (CASE type
                     WHEN 'ETH'::ucg_currency_type THEN 21000
                     WHEN 'ERC20'::ucg_currency_type THEN 100000
                 END)
        CHECK (CASE type
                   WHEN 'ETH'::ucg_currency_type THEN
                       transfer_gas_limit = 21000
                   WHEN 'ERC20'::ucg_currency_type THEN
                       transfer_gas_limit IS NULL OR transfer_gas_limit > 21000
               END),
    DROP CONSTRAINT "Mandatory requirements for verified data";

ALTER TABLE ucg_currency
    ALTER COLUMN transfer_gas_limit DROP DEFAULT,
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
                               -- For ETH, no extra requirements
                               TRUE
                           WHEN 'ERC20'::ucg_currency_type THEN
                               transfer_gas_limit IS NOT NULL AND
                               decimals IS NOT NULL
                           ELSE FALSE
                       END
               END);
