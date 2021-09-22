-- Initial implementation only; for latest up-to-date implementation, see the repeatable migration
CREATE OR REPLACE FUNCTION ucp_insert_missing_currency_tracked_address_progress_rows()
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


-- Initial implementation only; for latest up-to-date implementation, see the repeatable migration
CREATE OR REPLACE FUNCTION ucp_insert_missing_currency_tracked_address_progress_trigger_st()
    RETURNS TRIGGER
    LANGUAGE plpgsql
AS
$$
BEGIN
    PERFORM ucp_insert_missing_currency_tracked_address_progress_rows();

    RETURN NEW;
END;
$$;


CREATE TRIGGER ucp_currency_insert_missing_m2m_tracked_address_rows_tr
    AFTER INSERT
    ON ucp_currency
    FOR EACH STATEMENT
EXECUTE PROCEDURE ucp_insert_missing_currency_tracked_address_progress_trigger_st();

CREATE TRIGGER ucp_tracked_address_insert_missing_m2m_currency_rows_tr
    AFTER INSERT
    ON ucp_tracked_address
    FOR EACH STATEMENT
EXECUTE PROCEDURE ucp_insert_missing_currency_tracked_address_progress_trigger_st();
