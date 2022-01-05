-- Initial implementation only; for latest up-to-date implementation, see the repeatable migration
CREATE OR REPLACE FUNCTION ucg_latest_block_with_eth_transfer_from_address(_address CHAR(42),
                                                                           _max_block INTEGER)
    RETURNS TABLE
            (
                block_number INTEGER
            )
    LANGUAGE SQL
    STABLE
AS
$$
SELECT
    MAX(block_number) AS block_number
FROM
    ucg_eth_transfer AS eth_transfer
WHERE
    "from" = _address AND
    block_number <= _max_block
$$;

-- Initial implementation only; for latest up-to-date implementation, see the repeatable migration
CREATE OR REPLACE FUNCTION ucg_latest_block_with_eth_transfer_to_address(_address CHAR(42),
                                                                         _max_block INTEGER)
    RETURNS TABLE
            (
                block_number INTEGER
            )
    LANGUAGE SQL
    STABLE

AS
$$
SELECT
    MAX(block_number) AS block_number
FROM
    ucg_eth_transfer AS eth_transfer
WHERE
    "to" = _address AND
    block_number <= _max_block
$$;

-- Initial implementation only; for latest up-to-date implementation, see the repeatable migration
CREATE OR REPLACE FUNCTION ucg_latest_block_with_eth_transfer_for_address(_address CHAR(42),
                                                                          _max_block INTEGER)
    RETURNS TABLE
            (
                block_number INTEGER
            )
    LANGUAGE SQL
    STABLE

AS
$$
SELECT
    GREATEST(_from.block_number, _to.block_number)
FROM
    ucg_latest_block_with_eth_transfer_from_address(_address, _max_block) AS _from
    CROSS JOIN ucg_latest_block_with_eth_transfer_to_address(_address, _max_block) AS _to
$$;

-- Initial implementation only; for latest up-to-date implementation, see the repeatable migration
CREATE OR REPLACE FUNCTION ucg_latest_block_with_verified_erc20_transfer_from_address(_currency_id INTEGER,
                                                                                      _address CHAR(42),
                                                                                      _max_block INTEGER)
    RETURNS TABLE
            (
                block_number INTEGER
            )
    LANGUAGE SQL
    STABLE
AS
$$
SELECT
    MAX(block_number) AS block_number
FROM
    ucg_erc20_transfer_for_verified_currency
WHERE
    "from" = _address AND
    currency_id = _currency_id AND
    block_number <= _max_block
$$;

-- Initial implementation only; for latest up-to-date implementation, see the repeatable migration
CREATE OR REPLACE FUNCTION ucg_latest_block_with_verified_erc20_transfer_to_address(_currency_id INTEGER,
                                                                                    _address CHAR(42),
                                                                                    _max_block INTEGER)
    RETURNS TABLE
            (
                block_number INTEGER
            )
    LANGUAGE SQL
    STABLE
AS
$$
SELECT
    MAX(block_number) AS block_number
FROM
    ucg_erc20_transfer_for_verified_currency
WHERE
    "to" = _address AND
    currency_id = _currency_id AND
    block_number <= _max_block
$$;

-- Initial implementation only; for latest up-to-date implementation, see the repeatable migration
CREATE OR REPLACE FUNCTION ucg_latest_block_with_verified_erc20_transfer_for_address(_currency_id INTEGER,
                                                                                     _address CHAR(42),
                                                                                     _max_block INTEGER)
    RETURNS TABLE
            (
                block_number INTEGER
            )
    LANGUAGE SQL
    STABLE
AS
$$
SELECT
    GREATEST(_from.block_number, _to.block_number)
FROM
    ucg_latest_block_with_verified_erc20_transfer_from_address(_currency_id, _address, _max_block) AS _from
    CROSS JOIN ucg_latest_block_with_verified_erc20_transfer_to_address(_currency_id, _address, _max_block) AS _to
$$;
