-- Initial implementation only; for latest up-to-date implementation, see the repeatable migration
CREATE OR REPLACE FUNCTION ucg_is_erc20_transfer_event(_topics BYTEA[])
    RETURNS BOOLEAN
    LANGUAGE PLPGSQL
    IMMUTABLE STRICT
AS
$$
BEGIN
    IF !ucg_are_all_topics_valid(_topics)
    THEN
        RAISE EXCEPTION 'Not a valid list of topics: %!', _topics;
    END IF;

    RETURN (
            (length(_topics) = 3) AND
            (_topics[1] = '\xddf252ad1be2c89b69c2b068fc378daa952ba7f163c4a11628f55a4df523b3ef'::bytea)
        );
END;
$$;

CREATE OR REPLACE FUNCTION ucg_erc20_transfer_event_get_from(_topics BYTEA[])
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

    RETURN ucg_uint256_to_address_string(_topics[2]);
END;
$$;

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
