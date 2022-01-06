CREATE OR REPLACE FUNCTION ucg_get_currencies_for_keys_filter(_has_filter_currency_keys BOOLEAN,
                                                              _filter_currency_keys TEXT[])
    RETURNS TABLE
            (
                currency_id INTEGER
            )
    LANGUAGE SQL
    STABLE
AS
$$
WITH
    vars AS (
        SELECT array_position(_filter_currency_keys, '') IS NOT NULL AS filter_contains_eth
    )
SELECT id AS currency_id
FROM
    vars,
    ucg_currency
WHERE
    -- let's calculate a condition which matches the currencies
    CASE _has_filter_currency_keys
        -- when we have filter - match the filter. Differently for ERC20 and ETH, though.
        WHEN TRUE
            -- two subconditions: either dapp address matches; or ETH matches
            THEN
                (dapp_address IN (
                    SELECT *
                    FROM unnest(_filter_currency_keys)
                )) OR
                (filter_contains_eth AND ucg_currency.type = 'ETH')
        ELSE -- when no filter, just not extra condition to filter
            TRUE
    END
$$;

COMMENT ON FUNCTION ucg_get_currencies_for_keys_filter(
    _has_filter_currency_keys BOOLEAN, _filter_currency_keys TEXT[]) IS
    '(Table-returning inlineable) function to find all the `currency_id` for a given filter of currency keys.';
