CREATE OR REPLACE VIEW ucg_erc20_transfer_for_verified_currency_tr_addr_w_balance
AS
    SELECT
        erc20.address,
        erc20.tx_log_id,
        erc20.transaction_id,
        erc20.transaction_hash,
        erc20.block_number,
        erc20.block_hash,
        erc20.timestamp,
        erc20.log_index,
        erc20."from",
        erc20."to",
        erc20.contract,
        erc20.value,
        erc20.value_human,
        erc20.balance_change,
        (SUM(balance_change)
         OVER (PARTITION BY address, contract
             ORDER BY block_number, log_index)) AS balance,

        erc20.currency_type,
        erc20.currency_name,
        erc20.currency_symbol
    FROM
        ucg_erc20_transfer_for_verified_currency_tr_addr erc20;

COMMENT ON VIEW ucg_erc20_transfer_for_verified_currency_tr_addr_w_balance IS
    'ERC20 Transfer event parsed data. '
        'Only the verified currencies are supported, and only for the addresses tracked by the system; '
        'so extra information is available, such as the value amount with proper decimals (`value_human`), '
        'as well as the balance change for the specific tracked address (`balance_change`) and the running total '
        'for the balance (`balance`).';

COMMENT ON COLUMN ucg_erc20_transfer_for_verified_currency_tr_addr_w_balance.address IS
    'A tracked address, for which the balance change is calculated.';
COMMENT ON COLUMN ucg_erc20_transfer_for_verified_currency_tr_addr_w_balance.from IS
    '`from` field of ERC20 Transfer event – the Ethereum address of the transfer sender.';
COMMENT ON COLUMN ucg_erc20_transfer_for_verified_currency_tr_addr_w_balance.to IS
    '`to` field of ERC20 Transfer event – the Ethereum address of the transfer receiver.';
COMMENT ON COLUMN ucg_erc20_transfer_for_verified_currency_tr_addr_w_balance.contract IS
    'The address of the ERC20 token contract (i.e. the asset being transferred).';
COMMENT ON COLUMN ucg_erc20_transfer_for_verified_currency_tr_addr_w_balance.value IS
    'The value being transferred; raw data as stored in the ERC20 Transfer event in UINT256 '
        '(i.e. without knowing the place of the decimal point).';
COMMENT ON COLUMN ucg_erc20_transfer_for_verified_currency_tr_addr_w_balance.value_human IS
    'The value being transferred, adapted to contain the proper decimal point.';
COMMENT ON COLUMN ucg_erc20_transfer_for_verified_currency_tr_addr_w_balance.balance_change IS
    'The balance change of token `contract` for the `address`.';
COMMENT ON COLUMN ucg_erc20_transfer_for_verified_currency_tr_addr_w_balance.balance IS
    'The current balance (at the specific block `block_number`, and, inside a block - at log `log_index`).';
