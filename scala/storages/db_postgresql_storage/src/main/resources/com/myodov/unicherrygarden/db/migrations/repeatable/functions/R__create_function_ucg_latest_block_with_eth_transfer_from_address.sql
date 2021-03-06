CREATE OR REPLACE FUNCTION ucg_latest_block_with_eth_transfer_from_address(_address CHAR(42),
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
    ucg_eth_transfer AS eth_transfer
WHERE
    "from" = _address AND
    block_number <= _max_block
$$;

CREATE OR REPLACE FUNCTION ucg_latest_block_with_eth_transfer_from_address(_currency_id INTEGER,
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
    ucg_eth_transfer AS eth_transfer
WHERE
    currency_id = _currency_id AND
    "from" = _address AND
    block_number <= _max_block
$$;


COMMENT ON FUNCTION ucg_latest_block_with_eth_transfer_from_address(
    _address CHAR(42), _max_block INTEGER) IS
    '(Table-returning inlineable) function to find the maximum block containing the valid ETH transfer '
        'from the requested address. '
        'Note: the block may contain multiple matching transactions!';

COMMENT ON FUNCTION ucg_latest_block_with_eth_transfer_from_address(
    _currency_id INTEGER, _address CHAR(42), _max_block INTEGER) IS
    '(Table-returning inlineable) function to find the maximum block containing the valid ETH transfer '
        'from the requested address. '
        'Note: the block may contain multiple matching transactions! '
        'Another note: assumes/requires the `_currency_id` passed is for ETH currency.';
