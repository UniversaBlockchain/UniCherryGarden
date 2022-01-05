-- Initial implementation only; for latest up-to-date implementation, see the repeatable migration

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

CREATE OR REPLACE FUNCTION ucg_erc20_transfer_event_get_value(_data BYTEA)
    RETURNS NUMERIC
    LANGUAGE SQL
    IMMUTABLE STRICT
AS
$$
SELECT ucg_uint256_to_numeric(_data)
$$;

CREATE OR REPLACE FUNCTION ucg_erc20_transfer_event_get_value(_data BYTEA, _decimals SMALLINT)
    RETURNS NUMERIC
    LANGUAGE SQL
    IMMUTABLE STRICT
AS
$$
SELECT ucg_erc20_transfer_event_get_value(_data) / power(10::numeric, _decimals::numeric)
$$;
