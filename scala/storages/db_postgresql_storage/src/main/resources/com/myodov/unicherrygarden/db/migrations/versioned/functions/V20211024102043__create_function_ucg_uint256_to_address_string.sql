-- Initial implementation only; for latest up-to-date implementation, see the repeatable migration
CREATE OR REPLACE FUNCTION ucg_uint256_to_address_string(_uint256 BYTEA)
    RETURNS CHAR(42)
    LANGUAGE PLPGSQL
    IMMUTABLE STRICT
AS
$$
BEGIN
    IF length(_uint256) != 32
    THEN
        RAISE EXCEPTION 'length(%) is % instead of 32!', _uint256, length(_uint256);
    END IF;

    IF NOT (substring(_uint256 FROM 1 FOR 12) = '\x000000000000000000000000')
    THEN
        RAISE EXCEPTION 'high 12 bytes of % must be zero bytes!', _uint256;
    END IF;

    RETURN (
        SELECT '0x' || encode(substring(_uint256 FROM 13), 'hex')
    );
END;
$$;
