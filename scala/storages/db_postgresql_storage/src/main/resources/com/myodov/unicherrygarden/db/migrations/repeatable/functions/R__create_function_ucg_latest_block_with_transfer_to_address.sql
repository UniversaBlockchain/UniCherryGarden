CREATE OR REPLACE FUNCTION ucg_latest_block_with_transfer_to_address(_currency_id INTEGER,
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
    coalesce(eth.block_number, erc20.block_number) AS block_number
FROM
    ucg_latest_block_with_eth_transfer_to_address(_currency_id,
                                                  _address,
                                                  _max_block) AS eth,
    ucg_latest_block_with_verified_erc20_transfer_to_address(_currency_id,
                                                             _address,
                                                             _max_block) AS erc20;
$$;

COMMENT ON FUNCTION ucg_latest_block_with_transfer_to_address(
    _currency_id INTEGER, _address CHAR(42), _max_block INTEGER) IS
    '(Table-returning inlineable) function to find the maximum block containing the valid ETH/ERC20 transfer '
        'to the requested address (for ETH or verified ERC20 token only). '
        'Note: the block may contain multiple matching transactions!';
