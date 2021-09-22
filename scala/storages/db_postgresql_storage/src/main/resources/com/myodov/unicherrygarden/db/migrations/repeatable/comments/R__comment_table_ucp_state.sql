COMMENT ON TABLE ucp_state IS
    'The global state of UniCherrypicker instance.';

COMMENT ON COLUMN ucp_state.restarted_at IS
    'Timestamp of last restart.';
COMMENT ON COLUMN ucp_state.last_heartbeat_at IS
    'Timestamp of last instance heartbeat (when it was running).';
COMMENT ON COLUMN ucp_state.sync_state IS
    'Overall state of syncing process.';
COMMENT ON COLUMN ucp_state.synced_from_block_number IS
    'First block number since which the UniCherrypicker is tracking the changes.';
COMMENT ON COLUMN ucp_state.synced_to_block_number IS
    'Last block number till which the UniCherrypicker is properly synced. NULL if syncing hasn''t started yet.';

COMMENT ON INDEX ucp_state_one_row IS
    'Ensures that the ucp_state table can have only one row.';
