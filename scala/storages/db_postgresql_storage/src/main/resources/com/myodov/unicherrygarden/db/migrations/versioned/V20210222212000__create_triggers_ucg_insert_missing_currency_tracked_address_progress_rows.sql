-- Initial implementation only; for latest up-to-date implementation, see the repeatable migration
CREATE OR REPLACE FUNCTION ucg_insert_missing_currency_tracked_address_progress_rows()
    RETURNS VOID
    LANGUAGE SQL
AS
$$
WITH
    data_to_insert(currency_id, tracked_address_id, synced_from_block_number) AS (
        SELECT
            ucg_currency.id AS currency_id,
            ucg_tracked_address.id AS tracked_address_id,
            ucg_currency.sync_from_block_number AS synced_from_block_number
        FROM
            ucg_currency
            CROSS JOIN ucg_tracked_address
        WHERE
                (ucg_currency.id, ucg_tracked_address.id)
                NOT IN (
                    SELECT currency_id, tracked_address_id
                    FROM ucg_currency_tracked_address_progress
                )
    )
INSERT
INTO
    ucg_currency_tracked_address_progress(currency_id, tracked_address_id, synced_from_block_number)
SELECT *
FROM data_to_insert;
$$;


-- Initial implementation only; for latest up-to-date implementation, see the repeatable migration
CREATE OR REPLACE FUNCTION ucg_insert_missing_currency_tracked_address_progress_trigger_st()
    RETURNS TRIGGER
    LANGUAGE plpgsql
AS
$$
BEGIN
    PERFORM ucg_insert_missing_currency_tracked_address_progress_rows();

    RETURN NEW;
END;
$$;


CREATE TRIGGER ucg_currency_insert_missing_m2m_tracked_address_rows_tr
    AFTER INSERT
    ON ucg_currency
    FOR EACH STATEMENT
EXECUTE PROCEDURE ucg_insert_missing_currency_tracked_address_progress_trigger_st();

CREATE TRIGGER ucg_tracked_address_insert_missing_m2m_currency_rows_tr
    AFTER INSERT
    ON ucg_tracked_address
    FOR EACH STATEMENT
EXECUTE PROCEDURE ucg_insert_missing_currency_tracked_address_progress_trigger_st();
