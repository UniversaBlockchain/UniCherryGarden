INSERT INTO
    ucg_currency(type, dapp_address, name, symbol, ucg_comment, sync_from_block_number)
VALUES
('ERC20', '0x9e3319636e2126e3c0bc9e3134aec5e1508a46c7', 'Universa UTNP', 'UTNP', 'UTNP token', 4902190);


INSERT INTO
    ucg_tracked_address(address, ucg_comment, synced_from_block_number)
VALUES
('0x884191033518be08616821d7676ca01695698451', 'Universa UTNP Cold Reserve', 4451119),
('0xd701edf8f9c5d834bcb9add73ddeff2d6b9c3d24', 'xchange UTNP hotswap address', 4902220),
('0x3452519f4711703e13ea0863487eb8401bd6ae57', 'Universa BulkSender', 4902220);
