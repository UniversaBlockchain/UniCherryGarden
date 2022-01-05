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
        tx.balance_change,
        (SUM(balance_change)
         OVER (PARTITION BY address
             ORDER BY block_number, transaction_index)) AS balance
    FROM
        ucg_eth_transfer_tr_addr tx;

COMMENT ON VIEW ucg_eth_transfer_tr_addr_w_balance IS
    'Ethereum transfer event for some transaction. '
        'Only the successful transactions that succeeded are supported, and only for the addresses tracked by the system; '
        'so extra information is available, such as the value in proper ETH form (`value_human`) instead of weis, '
        'and similarly the amount of fees spent (`fees_total_human`), '
        'as well as the balance change for the specific tracked address (`balance_change`) and the running total '
        'for the balance (`balance`).';

COMMENT ON COLUMN ucg_eth_transfer_tr_addr_w_balance.nonce IS
    'Value of nonce of the transaction; from eth.getTransaction().';
COMMENT ON COLUMN ucg_eth_transfer_tr_addr_w_balance.txhash IS
    'The hash/globally unique address of the transaction.';
COMMENT ON COLUMN ucg_eth_transfer_tr_addr_w_balance.block_number IS
    'In what transaction the block has been mined; from eth.getTransaction().';
COMMENT ON COLUMN ucg_eth_transfer_tr_addr_w_balance.transaction_index IS
    'The index of the transaction inside the block.';
COMMENT ON COLUMN ucg_eth_transfer_tr_addr_w_balance.from IS
    'The address of the sender of the transaction.';
COMMENT ON COLUMN ucg_eth_transfer_tr_addr_w_balance.to IS
    'The address of the receiver of the transaction.';
COMMENT ON COLUMN ucg_eth_transfer_tr_addr_w_balance.transaction_status IS
    'Transaction status code; EIP 658, available in transactions only since Byzantium fork, since block 4,370,000. '
        '(1 for success, 0 for failure). NULL in transactions before Byzantium.';
COMMENT ON COLUMN ucg_eth_transfer_tr_addr_w_balance.transaction_ucg_comment IS
    '`ucg_comment` field of the transaction: comment on the transaction, manually entered by UniCherryGarden admins.';
COMMENT ON COLUMN ucg_eth_transfer_tr_addr_w_balance.value IS
    'The value being transferred, in weis; raw data as stored in the transaction in UINT256.';
COMMENT ON COLUMN ucg_eth_transfer_tr_addr_w_balance.value_human IS
    'The value being transferred, in ETH.';
COMMENT ON COLUMN ucg_eth_transfer_tr_addr_w_balance.fees_total IS
    'The total amount of fees paid, in weis; raw data as calculated in UINT256.';
COMMENT ON COLUMN ucg_eth_transfer_tr_addr_w_balance.fees_total_human IS
    'The total amount of fees paid, in ETH.';
COMMENT ON COLUMN ucg_eth_transfer_tr_addr_w_balance.balance_change IS
    'The balance change (in ETH) for the `address`.';
COMMENT ON COLUMN ucg_eth_transfer_tr_addr_w_balance.balance IS
    'The current balance (at the specific block `block_number`, '
        'and, inside a block - at transaction index `transaction_index`).';
