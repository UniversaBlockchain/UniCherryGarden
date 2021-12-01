CREATE OR REPLACE VIEW ucg_erc20_transfer_for_verified_currency AS
    SELECT
        ucg_erc20_transfer.tx_log_id,
        ucg_erc20_transfer.transaction_id,
        ucg_erc20_transfer.block_number,
        ucg_erc20_transfer.log_index,
        ucg_erc20_transfer.from,
        ucg_erc20_transfer.to,
        ucg_erc20_transfer.contract,
        ucg_erc20_transfer.value,
        (ucg_erc20_transfer.value / power(10::numeric, ucg_currency.decimals::numeric))
            AS value_human,
        ucg_currency.type AS currency_type,
        ucg_currency.name AS currency_name,
        ucg_currency.symbol AS currency_symbol
    FROM
        ucg_erc20_transfer
        INNER JOIN ucg_currency
                   ON ucg_erc20_transfer.contract = ucg_currency.dapp_address
    WHERE
        ucg_currency.verified;
