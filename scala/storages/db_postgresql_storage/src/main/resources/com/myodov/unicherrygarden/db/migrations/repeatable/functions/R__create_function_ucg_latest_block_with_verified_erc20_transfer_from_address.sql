CREATE OR REPLACE FUNCTION ucg_latest_block_with_verified_erc20_transfer_from_address(_currency_id INTEGER,
                                                                                      _address CHAR(42),
                                                                                      _max_block INTEGER)
    RETURNS TABLE
            (
                block_number INTEGER
            )
    LANGUAGE SQL
    STABLE
AS
$$
SELECT
    MAX(block_number) AS block_number
FROM
    ucg_erc20_transfer_for_verified_currency
WHERE
    "from" = _address AND
    currency_id = _currency_id AND
    block_number <= _max_block
$$;


COMMENT ON FUNCTION ucg_latest_block_with_verified_erc20_transfer_from_address(
    _currency_id INTEGER, _address CHAR(42), _max_block INTEGER) IS
    '(Table-returning inlineable) function to find the maximum block containing the valid ERC20 transfer '
        'from the requested address (for verified ERC20 token only). '
        'Note: the block may contain multiple matching transactions!';
