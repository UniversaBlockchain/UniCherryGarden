COMMENT ON TABLE ucp_block IS
    'A block in Ethereum blockchain.';

COMMENT ON COLUMN ucp_block.number IS
    'Block number; from 0 and higher, increasing.';
COMMENT ON COLUMN ucp_block.hash IS
    'Hash of the block.';
COMMENT ON COLUMN ucp_block.parent_hash IS
    'Hash of the parent block (or NULL if we don''t know a parent block).';
COMMENT ON COLUMN ucp_block.timestamp IS
    'Timestamp of the block.';
COMMENT ON COLUMN ucp_block.ucp_comment IS
    'Comment on currency manually entered by UniCherrypicker admins.';
