CREATE OR REPLACE VIEW ucg_erc20_transfer AS
    SELECT
        ucg_tx_log.id AS tx_log_id,
        ucg_tx_log.transaction_id,
        ucg_tx_log.block_number,
        ucg_tx_log.log_index,
        ucg_erc20_transfer_event_get_from(topics) AS from,
        ucg_erc20_transfer_event_get_to(topics) AS to,
        ucg_transaction.to_hash AS contract,
        ucg_erc20_transfer_event_get_value(data) AS value
    FROM
        ucg_tx_log
        INNER JOIN ucg_transaction
                   ON ucg_tx_log.transaction_id = ucg_transaction.id
    WHERE
        ucg_is_erc20_transfer_event(ucg_tx_log.topics) AND
        -- Double-checking the block number
        ucg_tx_log.block_number = ucg_transaction.block_number;
