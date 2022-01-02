DROP VIEW ucg_erc20_transfer_for_verified_currency;

DROP VIEW ucg_erc20_transfer;

CREATE OR REPLACE VIEW ucg_erc20_transfer
            (tx_log_id,
             transaction_id, transaction_hash,
             block_number, block_hash, timestamp,
             log_index,
             "from", "to", contract,
             value)
AS
    SELECT
        ucg_tx_log.id AS tx_log_id,
        ucg_tx_log.transaction_id,
        ucg_transaction.txhash AS transaction_hash,
        ucg_tx_log.block_number,
        ucg_block.hash AS block_hash,
        ucg_block.timestamp AS timestamp,
        ucg_tx_log.log_index,
        ucg_erc20_transfer_event_get_from(ucg_tx_log.topics) AS "from",
        ucg_erc20_transfer_event_get_to(ucg_tx_log.topics) AS "to",
        ucg_tx_log.address AS contract,
        ucg_erc20_transfer_event_get_value(ucg_tx_log.data) AS value
    FROM
        ucg_tx_log
        INNER JOIN ucg_transaction
                   ON ucg_tx_log.transaction_id = ucg_transaction.id
        INNER JOIN ucg_block
                   ON ucg_transaction.block_number = ucg_block.number
    WHERE
        (ucg_transaction.status IS NULL OR ucg_transaction.status = 1) AND
        ucg_is_erc20_transfer_event(ucg_tx_log.topics) AND
        ucg_tx_log.block_number = ucg_transaction.block_number;

CREATE OR REPLACE VIEW ucg_erc20_transfer_for_verified_currency
            (tx_log_id,
             transaction_id, transaction_hash,
             block_number, block_hash, timestamp,
             log_index,
             "from", "to", contract,
             value, value_human,
             currency_type, currency_name, currency_symbol)
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
