CREATE OR REPLACE FUNCTION ucg_erc20_transfer_event_get_from(_topics BYTEA[])
    RETURNS CHAR(42)
    LANGUAGE SQL
    IMMUTABLE STRICT
AS
$$
SELECT ucg_uint256_to_address_string((ucg_ensure_topics_are_erc20_transfer_event(_topics))[2])
$$;

COMMENT ON FUNCTION ucg_erc20_transfer_event_get_from(_topics BYTEA[]) IS
    'For the list of topics in a ERC20 token Transfer event, get the "from" address string. '
        'Will fail if it is not a ERC20 Transfer event, or in case of any other error.';
