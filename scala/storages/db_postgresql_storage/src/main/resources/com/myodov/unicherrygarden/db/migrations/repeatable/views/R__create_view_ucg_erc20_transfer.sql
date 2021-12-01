COMMENT ON VIEW ucg_erc20_transfer IS
    'ERC20 Transfer event parsed data.';

COMMENT ON COLUMN ucg_erc20_transfer.from IS
    '`from` field of ERC20 Transfer event – the Ethereum address of the transfer sender.';
COMMENT ON COLUMN ucg_erc20_transfer.to IS
    '`to` field of ERC20 Transfer event – the Ethereum address of the transfer receiver.';
COMMENT ON COLUMN ucg_erc20_transfer.contract IS
    'The address of the ERC20 token contract (i.e. the asset being transferred).';
COMMENT ON COLUMN ucg_erc20_transfer.value IS
    'The value being transferred; raw data as stored in the ERC20 Transfer event in UINT256 '
        '(i.e. without knowing the place of the decimal point).';
