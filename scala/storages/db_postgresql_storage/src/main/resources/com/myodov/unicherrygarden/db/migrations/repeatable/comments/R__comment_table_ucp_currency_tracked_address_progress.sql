COMMENT ON TABLE ucp_currency_tracked_address_progress IS
    'The progress of syncing a particular tracked address against a particular currency.';

COMMENT ON COLUMN ucp_currency_tracked_address_progress.synced_from_block_number IS
    'First block number since which the UniCherrypicker has synced this tracked address for this currency.';
COMMENT ON COLUMN ucp_currency_tracked_address_progress.synced_to_block_number IS
    'Last block number till which the UniCherrypicker has synced this tracked address for this currency. NULL if syncing hasn''t started yet.';
