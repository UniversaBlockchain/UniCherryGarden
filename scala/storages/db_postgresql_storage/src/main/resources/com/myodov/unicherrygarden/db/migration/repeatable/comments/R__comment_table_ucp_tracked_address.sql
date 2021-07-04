COMMENT ON TABLE ucp_tracked_address IS
    'An Ethereum address that is being tracked for any transactions and transfers.';

COMMENT ON COLUMN ucp_tracked_address.id IS
    'Primary key.';
COMMENT ON COLUMN ucp_tracked_address.address IS
    'Hex address that is being tracked.';
COMMENT ON COLUMN ucp_tracked_address.ucp_comment IS
    'Comment on tracked address, manually entered by UniCherrypicker admins.';
COMMENT ON COLUMN ucp_tracked_address.synced_from_block_number IS
    'First block number since which the tracked address is being synced.';
COMMENT ON COLUMN ucp_tracked_address.synced_to_block_number IS
    'Last block number till which the tracked address is properly synced. NULL if syncing hasn''t started yet.';
