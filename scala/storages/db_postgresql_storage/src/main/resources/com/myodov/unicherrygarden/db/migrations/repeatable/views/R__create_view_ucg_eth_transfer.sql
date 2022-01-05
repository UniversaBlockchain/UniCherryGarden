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

COMMENT ON VIEW ucg_eth_transfer IS
    'Ethereum transfer event for some transaction. '
        'Only the successful transactions that succeeded are supported; '
        'extra information is available, such as the value in proper ETH form (`value_human`) instead of weis, '
        'and similarly the amount of fees spent (`fees_total_human`).';

COMMENT ON COLUMN ucg_eth_transfer.nonce IS
    'Value of nonce of the transaction; from eth.getTransaction().';
COMMENT ON COLUMN ucg_eth_transfer.txhash IS
    'The hash/globally unique address of the transaction.';
COMMENT ON COLUMN ucg_eth_transfer.block_number IS
    'In what transaction the block has been mined; from eth.getTransaction().';
COMMENT ON COLUMN ucg_eth_transfer.transaction_index IS
    'The index of the transaction inside the block.';
COMMENT ON COLUMN ucg_eth_transfer.from IS
    'The address of the sender of the transaction.';
COMMENT ON COLUMN ucg_eth_transfer.to IS
    'The address of the receiver of the transaction.';
COMMENT ON COLUMN ucg_eth_transfer.transaction_status IS
    'Transaction status code; EIP 658, available in transactions only since Byzantium fork, since block 4,370,000. '
        '(1 for success, 0 for failure). NULL in transactions before Byzantium.';
COMMENT ON COLUMN ucg_eth_transfer.transaction_ucg_comment IS
    '`ucg_comment` field of the transaction: comment on the transaction, manually entered by UniCherryGarden admins.';
COMMENT ON COLUMN ucg_eth_transfer.value IS
    'The value being transferred, in weis; raw data as stored in the transaction in UINT256.';
COMMENT ON COLUMN ucg_eth_transfer.value_human IS
    'The value being transferred, in ETH.';
COMMENT ON COLUMN ucg_eth_transfer.fees_total IS
    'The total amount of fees paid, in weis; raw data as calculated in UINT256.';
COMMENT ON COLUMN ucg_eth_transfer.fees_total_human IS
    'The total amount of fees paid, in ETH.';
