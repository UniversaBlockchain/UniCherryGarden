ALTER TABLE ucg_currency
    ADD COLUMN transfer_gas_limit BIGINT;

UPDATE ucg_currency
SET
    transfer_gas_limit = (CASE type
                              WHEN 'ETH'::ucg_currency_type THEN 21000
                              WHEN 'ERC20'::ucg_currency_type THEN 70000
                          END);

ALTER TABLE ucg_currency
    DROP CONSTRAINT ucg_currency_check,
    DROP CONSTRAINT "Mandatory requirements for verified data";

ALTER TABLE ucg_currency
    ADD CONSTRAINT decimals_check
        CHECK (CASE type
                   WHEN 'ETH'::ucg_currency_type THEN (decimals IS NULL)
                   WHEN 'ERC20'::ucg_currency_type THEN ((decimals IS NULL) OR ((decimals >= 0) AND (decimals <= 79)))
                   ELSE FALSE
               END),
    ADD CONSTRAINT transfer_gas_limit_check
        CHECK (CASE type
                   WHEN 'ETH'::ucg_currency_type THEN
                       transfer_gas_limit = 21000
                   WHEN 'ERC20'::ucg_currency_type THEN
                       transfer_gas_limit IS NULL OR transfer_gas_limit > 21000
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
                               -- For ETH, no extra requirements
                               TRUE
                           WHEN 'ERC20'::ucg_currency_type THEN
                                   transfer_gas_limit IS NOT NULL AND
                                   decimals IS NOT NULL
                           ELSE FALSE
                       END
               END);
