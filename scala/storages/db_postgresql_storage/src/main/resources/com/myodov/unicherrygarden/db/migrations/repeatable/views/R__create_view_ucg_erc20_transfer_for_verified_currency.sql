CREATE OR REPLACE VIEW ucg_erc20_transfer_for_verified_currency
            (tx_log_id, transaction_id, block_number, log_index, "from", "to", contract, value, value_human,
             currency_type, currency_name, currency_symbol)
AS
    SELECT
        ucg_erc20_transfer.tx_log_id,
        ucg_erc20_transfer.transaction_id,
        ucg_erc20_transfer.block_number,
        ucg_erc20_transfer.log_index,
        ucg_erc20_transfer."from",
        ucg_erc20_transfer."to",
        ucg_erc20_transfer.contract,
        ucg_erc20_transfer.value,
            ucg_erc20_transfer.value / power(10::numeric, ucg_currency.decimals::numeric) AS value_human,
        ucg_currency.type AS currency_type,
        ucg_currency.name AS currency_name,
        ucg_currency.symbol AS currency_symbol
    FROM
        ucg_erc20_transfer
        INNER JOIN ucg_currency
             ON ucg_erc20_transfer.contract = ucg_currency.dapp_address
    WHERE
        ucg_currency.verified;


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
