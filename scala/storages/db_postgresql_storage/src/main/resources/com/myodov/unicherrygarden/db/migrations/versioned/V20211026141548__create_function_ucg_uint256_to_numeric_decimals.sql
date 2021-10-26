-- Initial implementation only; for latest up-to-date implementation, see the repeatable migration
CREATE OR REPLACE FUNCTION ucg_uint256_to_numeric(_uint256 BYTEA, _decimals SMALLINT)
    RETURNS NUMERIC
    LANGUAGE PLPGSQL
    IMMUTABLE STRICT
AS
$$
BEGIN
    IF length(_uint256) != 32
    THEN
        RAISE EXCEPTION 'Not a valid topics list for ERC20 Transfer event: %!', _uint256;
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
            ucg_uint256_to_numeric(_uint256)::numeric(158, 79)
            /
            10::numeric ^ _decimals::numeric
        );
END;
$$;
