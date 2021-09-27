COMMENT ON TABLE ucg_currency IS
    'Any cryptocurrency or token detected in Ethereum blockchain.';

COMMENT ON COLUMN ucg_currency.id IS
    'Primary key.';
COMMENT ON COLUMN ucg_currency.type IS
    'Type of the currency (ETH, ERC20,...).';
COMMENT ON COLUMN ucg_currency.dapp_address IS
    'Hex address of the underlying dApp (if applicable). NULL for ETH.';
COMMENT ON COLUMN ucg_currency.name IS
    'Name of the currency (e.g. “UTN-P: Universa Token”). Non-unique and can actually be missing.';
COMMENT ON COLUMN ucg_currency.symbol IS
    'Symbol/ticker code of the currency (e.g. “UTNP”). Non-unique and can actually be missing.';
COMMENT ON COLUMN ucg_currency.ucg_comment IS
    'Comment on currency, manually entered by UniCherryGarden admins.';
COMMENT ON COLUMN ucg_currency.sync_from_block_number IS
    'First block number since which the currency should be synced.';
