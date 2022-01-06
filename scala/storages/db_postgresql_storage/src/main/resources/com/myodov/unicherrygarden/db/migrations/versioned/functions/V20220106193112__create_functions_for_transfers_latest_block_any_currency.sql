-- Initial implementation only; for latest up-to-date implementation, see the repeatable migration
CREATE OR REPLACE FUNCTION ucg_latest_block_with_eth_transfer_from_address(_currency_id INTEGER,
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
    ucg_eth_transfer AS eth_transfer
WHERE
    currency_id = _currency_id AND
    "from" = _address AND
    block_number <= _max_block
$$;

-- Initial implementation only; for latest up-to-date implementation, see the repeatable migration
CREATE OR REPLACE FUNCTION ucg_latest_block_with_eth_transfer_to_address(_currency_id INTEGER,
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
    ucg_eth_transfer AS eth_transfer
WHERE
    currency_id = _currency_id AND
    "to" = _address AND
    block_number <= _max_block
$$;

-- Initial implementation only; for latest up-to-date implementation, see the repeatable migration
CREATE OR REPLACE FUNCTION ucg_latest_block_with_eth_transfer_for_address(_currency_id INTEGER,
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
    ucg_latest_block_with_eth_transfer_from_address(_currency_id, _address, _max_block) AS _from
    CROSS JOIN ucg_latest_block_with_eth_transfer_to_address(_currency_id, _address, _max_block) AS _to
$$;


-- Initial implementation only; for latest up-to-date implementation, see the repeatable migration
CREATE OR REPLACE FUNCTION ucg_latest_block_with_transfer_from_address(_currency_id INTEGER,
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
    coalesce(eth.block_number, erc20.block_number) AS block_number
FROM
    ucg_latest_block_with_eth_transfer_from_address(_currency_id,
                                                    _address,
                                                    _max_block) AS eth,
    ucg_latest_block_with_verified_erc20_transfer_from_address(_currency_id,
                                                               _address,
                                                               _max_block) AS erc20;
$$;

-- Initial implementation only; for latest up-to-date implementation, see the repeatable migration
CREATE OR REPLACE FUNCTION ucg_latest_block_with_transfer_to_address(_currency_id INTEGER,
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
    coalesce(eth.block_number, erc20.block_number) AS block_number
FROM
    ucg_latest_block_with_eth_transfer_to_address(_currency_id,
                                                  _address,
                                                  _max_block) AS eth,
    ucg_latest_block_with_verified_erc20_transfer_to_address(_currency_id,
                                                             _address,
                                                             _max_block) AS erc20;
$$;


-- Initial implementation only; for latest up-to-date implementation, see the repeatable migration
CREATE OR REPLACE FUNCTION ucg_latest_block_with_transfer_for_address(_currency_id INTEGER,
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
    coalesce(eth.block_number, erc20.block_number) AS block_number
FROM
    ucg_latest_block_with_eth_transfer_for_address(_currency_id,
                                                   _address,
                                                   _max_block) AS eth,
    ucg_latest_block_with_verified_erc20_transfer_for_address(_currency_id,
                                                              _address,
                                                              _max_block) AS erc20;
$$;
