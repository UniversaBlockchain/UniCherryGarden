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

        erc20.currency_type,
        erc20.currency_name,
        erc20.currency_symbol
    FROM
        ucg_erc20_transfer_for_verified_currency_tr_addr erc20;
