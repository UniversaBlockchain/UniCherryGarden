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
COMMENT ON COLUMN ucg_currency.verified IS
    'Whether the token has been manually verified and officially listed at UniCherryGarden. '
        'To list it, the operator should validate the source code. '
        'The `decimals` must be specified explicitly; '
        'The `transfer_gas_limit` must be specified explicitly; '
        'ERC20 compatibility should be checked, and it should be ensured that it generates the proper '
        'ERC20 Transfer event, with the needed signature and fields, and in all the cases whenever a transfer happens.';
COMMENT ON COLUMN ucg_currency.decimals IS
    '“Decimals” value for the currency or token. '
        'Not used for ETH (cause it is builtin as “18” so all the calculations can be adapted immediately). '
        'Should be used for all the ERC20 tokens whenever possible, and is mandatory for “verified” ERC20 tokens.';
COMMENT ON COLUMN ucg_currency.transfer_gas_limit IS
    'Default gas limit for transfer operations.'
        'Defaults to 21000 for ETH. '
        'Should be used for all the ERC20 tokens whenever possible, and is mandatory for “verified” ERC20 tokens '
        '(expected to be larger than 21000 for ERC20 tokens).';
