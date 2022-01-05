CREATE OR REPLACE FUNCTION ucg_erc20_transfer_event_get_value(_data BYTEA)
    RETURNS NUMERIC
    LANGUAGE SQL
    IMMUTABLE
AS
$$
SELECT ucg_uint256_to_numeric(_data)
$$;

CREATE OR REPLACE FUNCTION ucg_erc20_transfer_event_get_value(_data BYTEA, _decimals SMALLINT)
    RETURNS NUMERIC
    LANGUAGE SQL
    IMMUTABLE
AS
$$
SELECT ucg_erc20_transfer_event_get_value(_data) / power(10::numeric, _decimals::numeric)
$$;

COMMENT ON FUNCTION ucg_erc20_transfer_event_get_value(_data BYTEA) IS
    'From the "data" field of the ERC20 Token Transfer event passed as _data, '
        'get the transferred value.';

COMMENT ON FUNCTION ucg_erc20_transfer_event_get_value(_data BYTEA, _decimals SMALLINT) IS
    'From the "data" field of the ERC20 Token Transfer event passed as _data, '
        'get the transferred value (shifted by _decimals decimal points).';
