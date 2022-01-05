CREATE OR REPLACE FUNCTION ucg_latest_block_with_eth_transfer_to_address(_address CHAR(42),
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
    "to" = _address AND
    block_number <= _max_block
$$;


COMMENT ON FUNCTION ucg_latest_block_with_eth_transfer_to_address(
    _address CHAR(42), _max_block INTEGER) IS
    '(Table-returning inlineable) function to find the maximum block containing the valid ETH transfer '
        'to the requested address. '
        'Note: the block may contain multiple matching transactions!';
