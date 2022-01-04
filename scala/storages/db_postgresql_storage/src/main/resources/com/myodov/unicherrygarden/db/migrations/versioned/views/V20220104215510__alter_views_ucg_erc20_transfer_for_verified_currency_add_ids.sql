DROP VIEW ucg_erc20_transfer_for_verified_currency_tr_addr_w_balance;
DROP VIEW ucg_erc20_transfer_for_verified_currency_tr_addr;
DROP VIEW ucg_erc20_transfer_for_verified_currency;

CREATE OR REPLACE VIEW ucg_erc20_transfer_for_verified_currency
            (tx_log_id,
             transaction_id, transaction_hash,
             block_number, block_hash, timestamp,
             log_index,
             "from", "to", contract,
             value, value_human,
             currency_id, currency_type, currency_name, currency_symbol)
AS
    SELECT
        ucg_erc20_transfer.tx_log_id,
        ucg_erc20_transfer.transaction_id,
        ucg_erc20_transfer.transaction_hash,
        ucg_erc20_transfer.block_number,
        ucg_erc20_transfer.block_hash,
        ucg_erc20_transfer.timestamp AS timestamp,
        ucg_erc20_transfer.log_index,
        ucg_erc20_transfer."from",
        ucg_erc20_transfer."to",
        ucg_erc20_transfer.contract,
        ucg_erc20_transfer.value,
        (ucg_erc20_transfer.value / power(10::numeric, ucg_currency.decimals::numeric)) AS value_human,
        ucg_currency.id AS currency_id,
        ucg_currency.type AS currency_type,
        ucg_currency.name AS currency_name,
        ucg_currency.symbol AS currency_symbol
    FROM
        ucg_erc20_transfer
        INNER JOIN ucg_currency
                   ON ucg_erc20_transfer.contract = ucg_currency.dapp_address
    WHERE
        ucg_currency.verified AND
        ucg_currency.type = 'ERC20';

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
        erc20.currency_id,
        erc20.currency_type,
        erc20.currency_name,
        erc20.currency_symbol
    FROM
        ucg_erc20_transfer_for_verified_currency_tr_addr erc20;
