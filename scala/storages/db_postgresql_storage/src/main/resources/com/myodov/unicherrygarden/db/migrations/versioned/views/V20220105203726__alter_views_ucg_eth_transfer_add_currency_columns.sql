DROP VIEW ucg_eth_transfer_tr_addr_w_balance;
DROP VIEW ucg_eth_transfer_tr_addr;
DROP VIEW ucg_eth_transfer;

CREATE OR REPLACE VIEW ucg_eth_transfer
AS
    SELECT
        tx.id AS transaction_id,
        tx.nonce,
        tx.txhash,
        tx.block_number,
        tx.transaction_index,
        tx.from,
        tx.to,
        tx.status AS transaction_status,
        tx.is_status_ok,
        tx.ucg_comment AS transaction_ucg_comment,
        tx.value,
        (value / power(10::numeric, 18::numeric)) AS value_human,
        (tx.gas_used * tx.effective_gas_price) AS fees_total,
        (tx.gas_used * tx.effective_gas_price / power(10::numeric, 18::numeric)) AS fees_total_human,
        currency.id AS currency_id,
        currency.type AS currency_type,
        currency.name AS currency_name,
        currency.symbol AS currency_symbol
    FROM
        ucg_currency AS currency
        CROSS JOIN ucg_transaction AS tx
    WHERE
        (currency.type = 'ETH') AND
        ((tx.status IS NULL) OR (tx.status = 1));

CREATE OR REPLACE VIEW ucg_eth_transfer_tr_addr
AS
    SELECT
        ucg_tracked_address.address,
        tx.transaction_id,
        tx.nonce,
        tx.txhash,
        tx.block_number,
        tx.transaction_index,
        tx.from,
        tx.to,
        tx.transaction_status,
        tx.is_status_ok,
        tx.transaction_ucg_comment,
        tx.value,
        tx.value_human,
        tx.fees_total,
        tx.fees_total_human,
        tx.currency_id,
        tx.currency_type,
        tx.currency_name,
        tx.currency_symbol,
        (
                CASE ucg_tracked_address.address = tx.to
                    WHEN TRUE THEN tx.value_human
                    ELSE 0
                END
                -
                CASE ucg_tracked_address.address = tx.from
                    WHEN TRUE THEN tx.value_human + tx.fees_total_human
                    ELSE 0
                END
            ) AS balance_change
    FROM
        ucg_tracked_address
        INNER JOIN ucg_eth_transfer AS tx
                   ON ucg_tracked_address.address = tx.from OR
                      ucg_tracked_address.address = tx.to;

CREATE OR REPLACE VIEW ucg_eth_transfer_tr_addr_w_balance
AS
    SELECT
        tx.address,
        tx.transaction_id,
        tx.nonce,
        tx.txhash,
        tx.block_number,
        tx.transaction_index,
        tx.from,
        tx.to,
        tx.transaction_status,
        tx.is_status_ok,
        tx.transaction_ucg_comment,
        tx.value,
        tx.value_human,
        tx.fees_total,
        tx.fees_total_human,
        tx.currency_id,
        tx.currency_type,
        tx.currency_name,
        tx.currency_symbol,
        tx.balance_change,
        (SUM(balance_change)
         OVER (PARTITION BY address
             ORDER BY block_number, transaction_index)) AS balance
    FROM
        ucg_eth_transfer_tr_addr tx;
