-- Initial implementation only; for latest up-to-date implementation, see the repeatable migration
CREATE OR REPLACE FUNCTION ucg_is_valid_hex_hash(_hash VARCHAR, _length INT)
    RETURNS BOOLEAN
    LANGUAGE SQL
    IMMUTABLE STRICT
AS
$$
SELECT
    (length(_hash) = _length AND
     _hash ~ '^0x[0-9a-f]+$') :: BOOLEAN
$$;
