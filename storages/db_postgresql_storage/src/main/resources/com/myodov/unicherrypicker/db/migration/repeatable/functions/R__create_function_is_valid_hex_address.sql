CREATE OR REPLACE FUNCTION ucp_is_valid_hex_address(_address VARCHAR, _length INT)
    RETURNS BOOLEAN
    LANGUAGE SQL
    IMMUTABLE STRICT
AS
$$
SELECT
    (length($1) = $2 AND
     (left($1, 2) = '0x' AND
      right($1, _length - 2) = lower(right($1, _length - 2)))) :: BOOLEAN
$$;

COMMENT ON FUNCTION ucp_is_valid_hex_address IS
    'Checks if the _address argument is a proper Ethereum-style hex address, in lowercase, with the length of _length.';
