CREATE OR REPLACE VIEW ucg_eth_transfer_tr_addr
AS
    SELECT
        ucg_tracked_address.address,
        tx.transaction_id,
        tx.nonce,
        tx.txhash,
        tx.block_number,
        tx.transaction_index,
        tx.from_hash,
        tx.to_hash,
        tx.transaction_status,
        tx.is_status_ok,
        tx.transaction_ucg_comment,
        tx.value,
        tx.value_human,
        tx.fees_total,
        tx.fees_total_human,
        (
                CASE ucg_tracked_address.address = tx.to_hash
                    WHEN TRUE THEN tx.value_human
                    ELSE 0
                END
                -
                CASE ucg_tracked_address.address = tx.from_hash
                    WHEN TRUE THEN tx.value_human + tx.fees_total_human
                    ELSE 0
                END
            ) AS balance_change
    FROM
        ucg_tracked_address
        INNER JOIN ucg_eth_transfer AS tx
                   ON ucg_tracked_address.address = tx.from_hash OR
                      ucg_tracked_address.address = tx.to_hash;

COMMENT ON VIEW ucg_eth_transfer_tr_addr IS
    'Ethereum transfer event for some transaction. '
        'Only the successful transactions that succeeded are supported, and only for the addresses tracked by the system; '
        'so extra information is available, such as the value in proper ETH form (`value_human`) instead of weis, '
        'and similarly the amount of fees spent (`fees_total_human`), '
        'as well as the balance change for the specific tracked address (`balance_change`) and the running total '
        'for the balance (`balance`).';

COMMENT ON COLUMN ucg_eth_transfer_tr_addr.nonce IS
    'Value of nonce of the transaction; from eth.getTransaction().';
COMMENT ON COLUMN ucg_eth_transfer_tr_addr.txhash IS
    'The hash/globally unique address of the transaction.';
COMMENT ON COLUMN ucg_eth_transfer_tr_addr.block_number IS
    'In what transaction the block has been mined; from eth.getTransaction().';
COMMENT ON COLUMN ucg_eth_transfer_tr_addr.transaction_index IS
    'The index of the transaction inside the block.';
COMMENT ON COLUMN ucg_eth_transfer_tr_addr.from_hash IS
    'The address of the sender of the transaction.';
COMMENT ON COLUMN ucg_eth_transfer_tr_addr.to_hash IS
    'The address of the receiver of the transaction.';
COMMENT ON COLUMN ucg_eth_transfer_tr_addr.transaction_status IS
    'Transaction status code; EIP 658, available in transactions only since Byzantium fork, since block 4,370,000. '
        '(1 for success, 0 for failure). NULL in transactions before Byzantium.';
COMMENT ON COLUMN ucg_eth_transfer_tr_addr.transaction_ucg_comment IS
    '`ucg_comment` field of the transaction: comment on the transaction, manually entered by UniCherryGarden admins.';
COMMENT ON COLUMN ucg_eth_transfer_tr_addr.value IS
    'The value being transferred, in weis; raw data as stored in the transaction in UINT256.';
COMMENT ON COLUMN ucg_eth_transfer_tr_addr.value_human IS
    'The value being transferred, in ETH.';
COMMENT ON COLUMN ucg_eth_transfer_tr_addr.fees_total IS
    'The total amount of fees paid, in weis; raw data as calculated in UINT256.';
COMMENT ON COLUMN ucg_eth_transfer_tr_addr.fees_total_human IS
    'The total amount of fees paid, in ETH.';
COMMENT ON COLUMN ucg_eth_transfer_tr_addr.balance_change IS
    'The balance change (in ETH) for the `address`.';
