UPDATE ucg_currency
SET
    verified = TRUE
WHERE
    type = 'ETH'::ucg_currency_type;
