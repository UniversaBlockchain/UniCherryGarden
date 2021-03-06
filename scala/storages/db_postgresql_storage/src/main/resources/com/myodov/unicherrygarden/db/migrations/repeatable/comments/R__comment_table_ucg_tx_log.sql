COMMENT ON TABLE ucg_tx_log IS
    'Each Ethereum transaction log generated by Ethereum transaction. '
        'Retrieved by eth.getTransactionReceipt(TXID) and stored in the `logs` section of receipt.';

COMMENT ON COLUMN ucg_tx_log.transaction_id IS
    'Foreign key to the transaction that generated this log.';
COMMENT ON COLUMN ucg_tx_log.block_number IS
    'The block in which the log has been generated. Should be unique together with `log_index`.';
COMMENT ON COLUMN ucg_tx_log.log_index IS
    'Should be unique together with `block_number`.';
COMMENT ON COLUMN ucg_tx_log.topics IS
    'The array of Ethereum log topics.';
COMMENT ON COLUMN ucg_tx_log.data IS
    'The contents of Ethereum log data field.';
COMMENT ON COLUMN ucg_tx_log.address IS
    'The address of the smart contract generating the event.';

COMMENT ON INDEX ucg_tx_log_erc20_address_from_to IS
    'Speed up queries on (address, from, to), (address, from), (address).';
COMMENT ON INDEX ucg_tx_log_erc20_from_to IS
    'Speed up queries on (from, to), (from).';
COMMENT ON INDEX ucg_tx_log_erc20_to_address IS
    'Speed up queries on (to, address), (to).';
