CREATE OR REPLACE VIEW ucg_progress AS
    WITH
        overall_state AS (
            SELECT
                synced_from_block_number AS overall_from -- nullable
            FROM ucg_state
        ),
        currency_state AS (
            SELECT
                MIN(sync_from_block_number) AS currency_sync_from_min, -- NOT NULL
                MAX(sync_from_block_number) AS currency_sync_from_max  -- NOT NULL
            FROM ucg_currency
        ),
        block_state AS (
            SELECT
                MIN(number) AS block_from, -- NULL
                MAX(number) AS block_to    -- NULL
            FROM ucg_block
        ),
        tracked_address_state AS (
            SELECT
                MIN(synced_from_block_number) AS address_from_min, -- NOT NULL
                MAX(synced_from_block_number) AS address_from_max, -- NOT NULL
                MIN(synced_to_block_number) AS address_to_min,     -- nullable
                MAX(synced_to_block_number) AS address_to_max      -- nullable
            FROM ucg_tracked_address
        ),
        tracked_address_synced_to_is_null_first AS (
            SELECT *
            FROM ucg_tracked_address
            WHERE synced_to_block_number IS NULL
            LIMIT 1
        ),
        tracked_address_has_nulls AS (
            SELECT count(*) > 0 AS address_to_has_nulls
            FROM tracked_address_synced_to_is_null_first
        ),
        currency_tracked_address_state AS (
            SELECT
                MIN(synced_from_block_number) AS currency_address_from_min,
                MAX(synced_from_block_number) AS currency_address_from_max,
                MIN(synced_to_block_number) AS currency_address_to_min,
                MAX(synced_to_block_number) AS currency_address_to_max
            FROM ucg_currency_tracked_address_progress
        ),
        currency_tracked_address_synced_to_is_null_first AS (
            SELECT *
            FROM ucg_currency_tracked_address_progress
            WHERE synced_to_block_number IS NULL
            LIMIT 1
        ),
        currency_tracked_address_has_nulls AS (
            SELECT count(*) > 0 AS currency_address_to_has_nulls
            FROM currency_tracked_address_synced_to_is_null_first
        )
    SELECT *
    FROM
        overall_state,
        currency_state,
        block_state,
        tracked_address_state,
        tracked_address_has_nulls,
        currency_tracked_address_state,
        currency_tracked_address_has_nulls;
