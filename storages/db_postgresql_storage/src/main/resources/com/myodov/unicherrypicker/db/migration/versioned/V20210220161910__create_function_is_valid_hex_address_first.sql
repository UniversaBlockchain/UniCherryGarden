CREATE OR REPLACE FUNCTION ucp_is_valid_hex_address(_address VARCHAR, _length INT)
    RETURNS BOOLEAN
    LANGUAGE SQL
    IMMUTABLE STRICT
AS
$$
SELECT FALSE -- dummy implementation
$$;
