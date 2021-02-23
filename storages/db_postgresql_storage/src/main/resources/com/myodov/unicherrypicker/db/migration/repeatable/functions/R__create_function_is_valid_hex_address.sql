CREATE OR REPLACE FUNCTION ucp_is_valid_hex_address(_address VARCHAR, _length INT)
    RETURNS BOOLEAN
    LANGUAGE SQL
    IMMUTABLE STRICT
AS
$$
SELECT
    (
        -- The length of the hash is correct,
            length($1) = $2 AND
            -- ... it starts from "0x"
            left($1, 2) = '0x' AND
            -- ... and after first two letters, all remaining letters are hexadecimal.
            right($1, _length - 2) ~ '^[0-9a-f]+$'
        ) :: BOOLEAN
$$;

COMMENT ON FUNCTION ucp_is_valid_hex_address IS
    'Checks if the _address argument is a proper Ethereum-style hex address, in lowercase, with the length of _length.';
