COMMENT ON TABLE ucg_transfer IS
    'A transfer of Ethereum-based currency.';

COMMENT ON COLUMN ucg_transfer.currency_id IS
    'What currency is being transfered.';
COMMENT ON COLUMN ucg_transfer.from_hash IS
    'Sender of the transfer (who is transferring the amount). May be NULL if it is a “mint” of currency.';
COMMENT ON COLUMN ucg_transfer.to_hash IS
    'Receiver of the transfer (who is receiving the amount). May be NULL if it is a “burn” of currency.';
COMMENT ON COLUMN ucg_transfer.ucg_comment IS
    'Comment on transfer, currency manually entered by UniCherryGarden admins.';
