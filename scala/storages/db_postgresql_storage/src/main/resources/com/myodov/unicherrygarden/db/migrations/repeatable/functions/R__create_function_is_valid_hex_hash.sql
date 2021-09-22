CREATE OR REPLACE FUNCTION ucp_is_valid_hex_hash(_hash VARCHAR, _length INT)
    RETURNS BOOLEAN
    LANGUAGE SQL
    IMMUTABLE STRICT
AS
$$
SELECT
    (length(_hash) = _length AND
     _hash ~ '^0x[0-9a-f]+$') :: BOOLEAN
$$;

COMMENT ON FUNCTION ucp_is_valid_hex_hash(_hash VARCHAR, _length INT) IS
    'Checks if the _address argument is a proper Ethereum-style hex address, in lowercase, with the length of _length.';
