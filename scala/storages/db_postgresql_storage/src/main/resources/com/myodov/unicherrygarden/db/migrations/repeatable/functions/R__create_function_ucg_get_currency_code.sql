CREATE OR REPLACE FUNCTION ucg_get_currency_code(_type ucg_currency.type%TYPE,
                                                 _dapp_address ucg_currency.dapp_address%TYPE)
    RETURNS char(42)
    LANGUAGE SQL
    IMMUTABLE
AS
$$
SELECT
    CASE _type
        WHEN 'ETH' THEN ''
        WHEN 'ERC20' THEN _dapp_address
    END
$$;

COMMENT ON FUNCTION ucg_get_currency_code(
    _type ucg_currency.type%TYPE,
    _dapp_address ucg_currency.dapp_address%TYPE) IS
    'Get the stable currency code for the currency';
