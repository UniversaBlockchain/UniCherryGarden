COMMENT ON TABLE ucg_tracked_address IS
    'An Ethereum address that is being tracked for any transactions and transfers.';

COMMENT ON COLUMN ucg_tracked_address.id IS
    'Primary key.';
COMMENT ON COLUMN ucg_tracked_address.address IS
    'Hex address that is being tracked.';
COMMENT ON COLUMN ucg_tracked_address.ucg_comment IS
    'Comment on tracked address, manually entered by UniCherryGarden admins.';
COMMENT ON COLUMN ucg_tracked_address.synced_from_block_number IS
    'First block number since which the tracked address is being synced.';
