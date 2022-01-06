CREATE OR REPLACE FUNCTION ucg_latest_block_with_eth_transfer_for_address(_address CHAR(42),
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
    GREATEST(_from.block_number, _to.block_number)
FROM
    ucg_latest_block_with_eth_transfer_from_address(_address, _max_block) AS _from
    CROSS JOIN ucg_latest_block_with_eth_transfer_to_address(_address, _max_block) AS _to
$$;

CREATE OR REPLACE FUNCTION ucg_latest_block_with_eth_transfer_for_address(_currency_id INTEGER,
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
    GREATEST(_from.block_number, _to.block_number)
FROM
    ucg_latest_block_with_eth_transfer_from_address(_currency_id, _address, _max_block) AS _from
    CROSS JOIN ucg_latest_block_with_eth_transfer_to_address(_currency_id, _address, _max_block) AS _to
$$;


COMMENT ON FUNCTION ucg_latest_block_with_eth_transfer_for_address(
    _address CHAR(42), _max_block INTEGER) IS
    '(Table-returning inlineable) function to find the maximum block containing the valid ETH transfer '
        'from or to the requested address. '
        'Note: the block may contain multiple matching transactions!';

COMMENT ON FUNCTION ucg_latest_block_with_eth_transfer_for_address(
    _currency_id INTEGER, _address CHAR(42), _max_block INTEGER) IS
    '(Table-returning inlineable) function to find the maximum block containing the valid ETH transfer '
        'from or to the requested address. '
        'Note: the block may contain multiple matching transactions! '
        'Another note: assumes/requires the `_currency_id` passed is for ETH currency.';
