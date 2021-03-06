CREATE INDEX IF NOT EXISTS ucg_tx_log_erc20_address_from_to
    ON ucg_tx_log (address,
                   ucg_erc20_transfer_event_get_from(topics),
                   ucg_erc20_transfer_event_get_to(topics))
    WHERE ucg_is_erc20_transfer_event(topics);

CREATE INDEX IF NOT EXISTS ucg_tx_log_erc20_from_to
    ON ucg_tx_log (ucg_erc20_transfer_event_get_from(topics),
                   ucg_erc20_transfer_event_get_to(topics))
    WHERE ucg_is_erc20_transfer_event(topics);

CREATE INDEX IF NOT EXISTS ucg_tx_log_erc20_to_address
    ON ucg_tx_log (ucg_erc20_transfer_event_get_to(topics),
                   address)
    WHERE ucg_is_erc20_transfer_event(topics);
