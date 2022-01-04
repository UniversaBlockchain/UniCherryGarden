CREATE OR REPLACE VIEW ucg_erc20_transfer_for_verified_currency_tr_addr
AS
    SELECT
        ucg_tracked_address.address,
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
        (
                CASE ucg_tracked_address.address = erc20."to"
                    WHEN TRUE THEN erc20.value_human
                    ELSE 0
                END
                -
                CASE ucg_tracked_address.address = erc20."from"
                    WHEN TRUE THEN erc20.value_human
                    ELSE 0
                END
            ) AS balance_change,
        erc20.currency_id,
        erc20.currency_type,
        erc20.currency_name,
        erc20.currency_symbol
    FROM
        ucg_tracked_address
        INNER JOIN ucg_erc20_transfer_for_verified_currency erc20
                   ON ucg_tracked_address.address = erc20."from" OR
                      ucg_tracked_address.address = erc20."to";

COMMENT ON VIEW ucg_erc20_transfer_for_verified_currency_tr_addr IS
    'ERC20 Transfer event parsed data. '
        'Only the verified currencies are supported, and only for the addresses tracked by the system; '
        'so extra information is available, such as the value amount with proper decimals (`value_human`), '
        'as well as the balance change for the specific tracked address (`balance_change`).';

COMMENT ON COLUMN ucg_erc20_transfer_for_verified_currency_tr_addr.address IS
    'A tracked address, for which the balance change is calculated.';
COMMENT ON COLUMN ucg_erc20_transfer_for_verified_currency_tr_addr.from IS
    '`from` field of ERC20 Transfer event – the Ethereum address of the transfer sender.';
COMMENT ON COLUMN ucg_erc20_transfer_for_verified_currency_tr_addr.to IS
    '`to` field of ERC20 Transfer event – the Ethereum address of the transfer receiver.';
COMMENT ON COLUMN ucg_erc20_transfer_for_verified_currency_tr_addr.contract IS
    'The address of the ERC20 token contract (i.e. the asset being transferred).';
COMMENT ON COLUMN ucg_erc20_transfer_for_verified_currency_tr_addr.value IS
    'The value being transferred; raw data as stored in the ERC20 Transfer event in UINT256 '
        '(i.e. without knowing the place of the decimal point).';
COMMENT ON COLUMN ucg_erc20_transfer_for_verified_currency_tr_addr.value_human IS
    'The value being transferred, adapted to contain the proper decimal point.';
COMMENT ON COLUMN ucg_erc20_transfer_for_verified_currency_tr_addr.balance_change IS
    'The balance change of token `contract` for the `address`.';
