CREATE OR REPLACE FUNCTION ucg_erc20_transfer_event_get_to(_topics BYTEA[])
    RETURNS CHAR(42)
    LANGUAGE PLPGSQL
    IMMUTABLE STRICT
AS
$$
BEGIN
    IF !ucg_is_erc20_transfer_event(_topics)
    THEN
        RAISE EXCEPTION 'Not a valid topics list for ERC20 Transfer event: %!', _topics;
    END IF;

    RETURN ucg_uint256_to_address_string(_topics[3]);
END;
$$;

COMMENT ON FUNCTION ucg_erc20_transfer_event_get_to(_topics BYTEA[]) IS
    'For the list of topics in a ERC20 token Transfer event, get the "to" address string. '
        'Will fail if it is not a ERC20 Transfer event, or in case of any other error.';
