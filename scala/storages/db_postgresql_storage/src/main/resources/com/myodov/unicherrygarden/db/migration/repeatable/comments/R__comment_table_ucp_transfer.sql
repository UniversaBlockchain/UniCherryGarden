COMMENT ON TABLE ucp_transfer IS
    'A transfer of Ethereum-based currency.';

COMMENT ON COLUMN ucp_transfer.currency_id IS
    'What currency is being transfered.';
COMMENT ON COLUMN ucp_transfer.from_hash IS
    'Sender of the transfer (who is transferring the amount). May be NULL if it is a “mint” of currency.';
COMMENT ON COLUMN ucp_transfer.to_hash IS
    'Receiver of the transfer (who is receiving the amount). May be NULL if it is a “burn” of currency.';
COMMENT ON COLUMN ucp_transfer.ucp_comment IS
    'Comment on transfer, currency manually entered by UniCherrypicker admins.';
