CREATE OR REPLACE VIEW ucg_erc20_transfer
            (tx_log_id, transaction_id, block_number, log_index, "from", "to", contract, value)
AS
    SELECT
        ucg_tx_log.id AS tx_log_id,
        ucg_tx_log.transaction_id,
        ucg_tx_log.block_number,
        ucg_tx_log.log_index,
        ucg_erc20_transfer_event_get_from(ucg_tx_log.topics) AS "from",
        ucg_erc20_transfer_event_get_to(ucg_tx_log.topics) AS "to",
        ucg_tx_log.address AS contract,
        ucg_erc20_transfer_event_get_value(ucg_tx_log.data) AS value
    FROM
        ucg_tx_log
        INNER JOIN ucg_transaction
             ON ucg_tx_log.transaction_id = ucg_transaction.id
    WHERE
        (ucg_transaction.status IS NULL OR ucg_transaction.status = 1) AND
        ucg_is_erc20_transfer_event(ucg_tx_log.topics) AND
        ucg_tx_log.block_number = ucg_transaction.block_number;


COMMENT ON VIEW ucg_erc20_transfer IS
    'ERC20 Transfer event parsed data. Only the transaction that succeeded are included.';

COMMENT ON COLUMN ucg_erc20_transfer.from IS
    '`from` field of ERC20 Transfer event – the Ethereum address of the transfer sender.';
COMMENT ON COLUMN ucg_erc20_transfer.to IS
    '`to` field of ERC20 Transfer event – the Ethereum address of the transfer receiver.';
COMMENT ON COLUMN ucg_erc20_transfer.contract IS
    'The address of the ERC20 token contract (i.e. the asset being transferred).';
COMMENT ON COLUMN ucg_erc20_transfer.value IS
    'The value being transferred; raw data as stored in the ERC20 Transfer event in UINT256 '
        '(i.e. without knowing the place of the decimal point).';
