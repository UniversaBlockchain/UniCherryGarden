COMMENT ON TABLE ucg_transaction IS
    'The Ethereum transaction. Contains the data from both eth.getTransaction() and eth.getTransactionReceipt().';

COMMENT ON COLUMN ucg_transaction.block_number IS
    'In what transaction the block has been mined; '
        'from eth.getTransaction(), though will be non-null only if the transaction is mined already.';

COMMENT ON COLUMN ucg_transaction.from IS
    'The address of the sender of the transaction.';
COMMENT ON COLUMN ucg_transaction.to IS
    'The address of the receiver of the transaction.';

COMMENT ON COLUMN ucg_transaction.status IS
    'Transaction status code; EIP 658, available in transactions only since Byzantium fork, since block 4,370,000. '
        '(1 for success, 0 for failure). NULL in transactions before Byzantium.';

COMMENT ON COLUMN ucg_transaction.ucg_comment IS
    'Comment on the transaction, manually entered by UniCherryGarden admins.';

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

COMMENT ON COLUMN ucg_transaction.transaction_index IS
    'Index of the transaction in the block; '
        'from eth.getTransaction(), though will be non-null only if the transaction is mined already.';
