ALTER TABLE ucg_state
    ADD COLUMN eth_node_blocknumber INTEGER DEFAULT 0 CHECK (eth_node_current_block >= 0),
    ADD COLUMN eth_node_current_block INTEGER DEFAULT 0 CHECK (eth_node_current_block >= 0),
    ADD COLUMN eth_node_highest_block INTEGER DEFAULT 0 CHECK (eth_node_current_block >= 0);
