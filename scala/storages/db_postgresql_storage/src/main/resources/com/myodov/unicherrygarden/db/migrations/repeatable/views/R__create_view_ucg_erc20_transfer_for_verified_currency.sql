COMMENT ON VIEW ucg_erc20_transfer_for_verified_currency IS
    'ERC20 Transfer event parsed data. '
        'Only the verified currencies are supported, so extra information is available, such as '
        'the value amount with proper decimals (`value_human`).';

COMMENT ON COLUMN ucg_erc20_transfer_for_verified_currency.from IS
    '`from` field of ERC20 Transfer event – the Ethereum address of the transfer sender.';
COMMENT ON COLUMN ucg_erc20_transfer_for_verified_currency.to IS
    '`to` field of ERC20 Transfer event – the Ethereum address of the transfer receiver.';
COMMENT ON COLUMN ucg_erc20_transfer_for_verified_currency.contract IS
    'The address of the ERC20 token contract (i.e. the asset being transferred).';
COMMENT ON COLUMN ucg_erc20_transfer_for_verified_currency.value IS
    'The value being transferred; raw data as stored in the ERC20 Transfer event in UINT256 '
        '(i.e. without knowing the place of the decimal point).';
COMMENT ON COLUMN ucg_erc20_transfer_for_verified_currency.value_human IS
    'The value being transferred, adapted to contain the proper decimal point.';
