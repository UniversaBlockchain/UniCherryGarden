COMMENT ON TABLE ucg_state IS
    'The global state of UniCherrypicker instance.';

COMMENT ON COLUMN ucg_state.restarted_at IS
    'Timestamp of last restart.';
COMMENT ON COLUMN ucg_state.last_heartbeat_at IS
    'Timestamp of last instance heartbeat (when it was running).';
COMMENT ON COLUMN ucg_state.sync_state IS
    'Overall state of syncing process.';
COMMENT ON COLUMN ucg_state.synced_from_block_number IS
    'First block number since which the UniCherrypicker is tracking the changes.';
COMMENT ON COLUMN ucg_state.eth_node_blocknumber IS
    '`eth.blockNumber` value on Ethereum node.';
COMMENT ON COLUMN ucg_state.eth_node_current_block IS
    '`eth.syncing.currentBlock` syncing value on Ethereum node; if eth.syncing is false, assumed equal to eth.blockNumber.';
COMMENT ON COLUMN ucg_state.eth_node_highest_block IS
    '`eth.syncing.highestBlock` syncing value on Ethereum node; if eth.syncing is false, assumed equal to eth.blockNumber.';

COMMENT ON INDEX ucg_state_one_row IS
    'Ensures that the ucg_state table can have only one row.';
