COMMENT ON TABLE ucp_block IS
    'A block in Ethereum blockchain.';

COMMENT ON COLUMN ucp_block.number IS
    'Block number; from 0 and higher, increasing. `Integer` instead of `BigInt` saves space, but must be fixed in 1657 years.';
COMMENT ON COLUMN ucp_block.hash IS
    'Hash of the block.';
COMMENT ON COLUMN ucp_block.parent_hash IS
    'Hash of the parent block.';
COMMENT ON COLUMN ucp_block.timestamp IS
    'Timestamp of the block.';
COMMENT ON COLUMN ucp_block.ucp_comment IS
    'Comment on block, currency manually entered by UniCherrypicker admins.';
