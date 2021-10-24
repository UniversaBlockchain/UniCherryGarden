CREATE OR REPLACE FUNCTION ucg_are_all_topics_valid(_topics BYTEA[])
    RETURNS BOOLEAN
    LANGUAGE SQL
    IMMUTABLE STRICT
AS
$$
SELECT
    (32 = ALL (SELECT LENGTH(topic) FROM unnest(_topics) AS topic))
$$;

COMMENT ON FUNCTION ucg_are_all_topics_valid(_topics BYTEA[]) IS
    'Checks if the array of Ethereum log topics (_topics) is valid.';
