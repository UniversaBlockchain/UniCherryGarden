-- Initial implementation only; for latest up-to-date implementation, see the repeatable migration
CREATE OR REPLACE FUNCTION ucg_are_all_topics_valid(_topics BYTEA[])
    RETURNS BOOLEAN
    LANGUAGE SQL
    IMMUTABLE STRICT
AS
$$
SELECT
    (32 = ALL (SELECT LENGTH(topic) FROM unnest(_topics) AS topic))
$$;
