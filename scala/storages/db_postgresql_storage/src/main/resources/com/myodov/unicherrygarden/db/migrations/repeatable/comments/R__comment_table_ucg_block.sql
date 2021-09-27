COMMENT ON TABLE ucg_block IS
    'A block in Ethereum blockchain.';

COMMENT ON COLUMN ucg_block.number IS
    'Block number; from 0 and higher, increasing. `Integer` instead of `BigInt` saves space, but must be fixed in 1657 years.';
COMMENT ON COLUMN ucg_block.hash IS
    'Hash of the block.';
COMMENT ON COLUMN ucg_block.parent_hash IS
    'Hash of the parent block.';
COMMENT ON COLUMN ucg_block.timestamp IS
    'Timestamp of the block.';
COMMENT ON COLUMN ucg_block.ucg_comment IS
    'Comment on block, currency manually entered by UniCherryGarden admins.';
