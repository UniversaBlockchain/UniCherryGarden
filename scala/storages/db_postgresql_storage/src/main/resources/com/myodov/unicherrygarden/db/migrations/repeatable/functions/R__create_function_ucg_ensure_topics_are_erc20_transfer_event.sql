CREATE OR REPLACE FUNCTION ucg_ensure_topics_are_erc20_transfer_event(_topics BYTEA[])
    RETURNS BYTEA[]
    LANGUAGE PLPGSQL
    IMMUTABLE STRICT
AS
$$
BEGIN
    IF NOT ucg_is_erc20_transfer_event(_topics)
    THEN
        RAISE EXCEPTION 'Not a valid topics list for ERC20 Transfer event: %!', _topics;
    END IF;

    RETURN _topics;
END;
$$;

COMMENT ON FUNCTION ucg_ensure_topics_are_erc20_transfer_event(_topics BYTEA[]) IS
    'Validate the topics part of the event; it must be a valid ERC20 Transfer event, '
        'otherwise an exception is raised';
