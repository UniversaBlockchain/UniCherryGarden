CREATE OR REPLACE FUNCTION ucp_is_valid_hex_hash(_hash VARCHAR, _length INT)
    RETURNS BOOLEAN
    LANGUAGE SQL
    IMMUTABLE STRICT
AS
$$
SELECT FALSE -- dummy implementation
$$;
