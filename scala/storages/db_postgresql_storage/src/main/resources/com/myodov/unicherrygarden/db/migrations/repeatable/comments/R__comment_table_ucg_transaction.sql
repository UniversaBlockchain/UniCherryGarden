COMMENT ON TABLE ucg_transaction IS
    'The Ethereum transaction. Contains the data from both eth.getTransaction() and eth.getTransactionReceipt().';

COMMENT ON COLUMN ucg_transaction.gas IS
    'Gas; from eth.getTransaction(). '
        'See transaction 0xeba1b33ad894aff5d6322f07b4b56d841f996bb2dc8d696eec3cdc552e4635a2 for minimal set of data.';
COMMENT ON COLUMN ucg_transaction.gas_price IS
    'Gas price; from eth.getTransaction().';

COMMENT ON COLUMN ucg_transaction.gas_used IS
    'Amount of gas used in the transaction; from eth.getTransactionReceipt().';
COMMENT ON COLUMN ucg_transaction.effective_gas_price IS
    'Effectively used gas price; from eth.getTransactionReceipt().';
COMMENT ON COLUMN ucg_transaction.cumulative_gas_used IS
    'Cumulative gas used; from eth.getTransactionReceipt().';

COMMENT ON COLUMN ucg_transaction.value IS
    'The passed value of ETH; from eth.getTransaction().';
COMMENT ON COLUMN ucg_transaction.nonce IS
    'Value of nonce; from eth.getTransaction().';

COMMENT ON COLUMN ucg_transaction.block_number IS
    'In what transaction the block has been mined; '
        'from eth.getTransaction(), though will be non-null only if the transaction is mined already.';
COMMENT ON COLUMN ucg_transaction.transaction_index IS
    'Index of the transaction in the block; '
        'from eth.getTransaction(), though will be non-null only if the transaction is mined already.';
