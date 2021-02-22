COMMENT ON TABLE ucp_currency IS
    'Any cryptocurrency or token detected in Ethereum blockchain.';

COMMENT ON COLUMN ucp_currency.id IS
    'Primary key.';
COMMENT ON COLUMN ucp_currency.type IS
    'Type of the currency (ETH, ERC20,...).';
COMMENT ON COLUMN ucp_currency.dapp_address IS
    'Hex address of the underlying dApp (if applicable). NULL for ETH.';
COMMENT ON COLUMN ucp_currency.name IS
    'Name of the currency (e.g. “UTN-P: Universa Token”). Non-unique and can actually be missing.';
COMMENT ON COLUMN ucp_currency.symbol IS
    'Symbol/ticker code of the currency (e.g. “UTNP”). Non-unique and can actually be missing.';
COMMENT ON COLUMN ucp_currency.ucp_comment IS
    'Comment on currency manually entered by UniCherrypicker admins.';
COMMENT ON COLUMN ucp_currency.synced_from_block_number IS
    'First block number since which the currency is being synced.';
COMMENT ON COLUMN ucp_currency.synced_to_block_number IS
    'Last block number till which the currency is properly synced. NULL if syncing hasn''t started yet.';
