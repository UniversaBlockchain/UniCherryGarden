COMMENT ON TABLE ucg_planted_transfer IS
    'Any new transfer transaction being planted in the blockchain.';

COMMENT ON COLUMN ucg_planted_transfer.added_at IS
    'Timestamp when the transfer has been added.';
COMMENT ON COLUMN ucg_planted_transfer.modified_at IS
    'Timestamp when the transfer has been last modified (if ever).';
COMMENT ON COLUMN ucg_planted_transfer.broadcasted_at IS
    'Timestamp when the transfer was last attempted to be broadcasted in the blockchain.';
COMMENT ON COLUMN ucg_planted_transfer.next_broadcast_at IS
    'Timestamp when the transfer must be attempted to be broadcasted in the blockchain (if needed; may be NULL).';

COMMENT ON COLUMN ucg_planted_transfer.data IS
    'Byte contents of the transaction to be broadcasted.';

COMMENT ON COLUMN ucg_planted_transfer.ucg_comment IS
    'Comment on the planted transfer, manually entered by UniCherryGarden admins.';
COMMENT ON COLUMN ucg_planted_transfer.error IS
    'Error message (if occurred during planting).';
