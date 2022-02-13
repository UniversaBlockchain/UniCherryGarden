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
SELECT id AS currency_id
FROM ucg_currency
WHERE
    -- let's calculate a condition which matches the currencies
    CASE _has_filter_currency_keys
        -- when we have filter - match the filter
        WHEN TRUE
            THEN
                ucg_get_currency_code(ucg_currency.type, ucg_currency.dapp_address) = ANY (_filter_currency_keys)
        ELSE -- when no filter, just not extra condition to filter
            TRUE
    END
$$;

COMMENT ON FUNCTION ucg_get_currencies_for_keys_filter(
    _has_filter_currency_keys BOOLEAN, _filter_currency_keys TEXT[]) IS
    '(Table-returning inlineable) function to find all the `currency_id` for a given filter of currency keys.';
