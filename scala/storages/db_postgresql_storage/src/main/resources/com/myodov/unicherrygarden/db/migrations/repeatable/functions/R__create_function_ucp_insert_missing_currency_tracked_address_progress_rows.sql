CREATE OR REPLACE FUNCTION c()
    RETURNS VOID
    LANGUAGE SQL
AS
$$
WITH
    data_to_insert(currency_id, tracked_address_id, synced_from_block_number) AS (
        SELECT
            ucp_currency.id AS currency_id,
            ucp_tracked_address.id AS tracked_address_id,
            ucp_currency.sync_from_block_number AS synced_from_block_number
        FROM
            ucp_currency
            CROSS JOIN ucp_tracked_address
        WHERE
                (ucp_currency.id, ucp_tracked_address.id)
                NOT IN (
                    SELECT currency_id, tracked_address_id
                    FROM ucp_currency_tracked_address_progress
                )
    )
INSERT
INTO
    ucp_currency_tracked_address_progress(currency_id, tracked_address_id, synced_from_block_number)
SELECT *
FROM data_to_insert;
$$;


CREATE OR REPLACE FUNCTION ucp_insert_missing_currency_tracked_address_progress_trigger_st()
    RETURNS TRIGGER
    LANGUAGE plpgsql
AS
$$
BEGIN
    EXECUTE ucp_insert_missing_currency_tracked_address_progress_rows();

    RETURN NEW;
END;
$$;

COMMENT ON FUNCTION ucp_insert_missing_currency_tracked_address_progress_rows() IS
    'Inserts all the missing records to M2M table ucp_currency_tracked_address_progress.';
