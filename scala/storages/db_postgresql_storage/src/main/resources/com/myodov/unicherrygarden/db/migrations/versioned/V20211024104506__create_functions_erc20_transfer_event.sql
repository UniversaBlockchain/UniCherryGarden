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

CREATE OR REPLACE FUNCTION ucg_erc20_transfer_event_get_display_value(_data BYTEA, _decimals SMALLINT)
    RETURNS NUMERIC
    LANGUAGE PLPGSQL
    IMMUTABLE STRICT
AS
$$
BEGIN
    IF length(_data) != 32
    THEN
        RAISE EXCEPTION 'Not a valid topics list for ERC20 Transfer event: %!', _data;
    END IF;

    IF NOT (0 <= _decimals AND _decimals <= 79)
    THEN
        RAISE EXCEPTION 'Decimals (%) must be between 0 and 79 inclusive!', _decimals;
    END IF;

    -- Result numeric should be in the following specification (numeric(precision, scale)):
    -- numeric(79*2, 79), i.e. numeric(158, 79).
    -- 79 – because (2**256 - 1), the maximum value handled, is no longer than 79 decimal symbols.
    -- This means that in `scale` there may be maximum 79 decimals, and we must never lose the precision.
    -- But there may be from 0 to 79 decimals, and 79 symbols at all, therefore
    -- `precision` should be twice than, i.e. 158

    RETURN (
            ucg_uint256_to_numeric(_data)::numeric(158, 79)
            /
            10::numeric ^ _decimals::numeric
        );
END;
$$;
