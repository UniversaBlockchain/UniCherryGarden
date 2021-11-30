CREATE OR REPLACE FUNCTION ucg_is_erc20_transfer_event(_topics BYTEA[])
    RETURNS BOOLEAN
    LANGUAGE SQL
    IMMUTABLE STRICT
AS
$$
SELECT
        (array_length(_topics, 1) = 3) AND
        (_topics[1] = '\xddf252ad1be2c89b69c2b068fc378daa952ba7f163c4a11628f55a4df523b3ef'::bytea)
$$;

COMMENT ON FUNCTION ucg_is_erc20_transfer_event(_topics BYTEA[]) IS
    'For the list of topics in a Ethereum log, detect if this is a valid ERC20 Transfer event (). '
        'The event must be a per-spec Transfer event, with the following definition: '
        'Transfer(address indexed _from, address indexed _to, uint256 _value). '
        'I.e. signature must be Transfer(address,address,uint256); and first two items must be indexed. '
        'Will fail if it is not a valid list of Ethereum topics (each of which is 32 bytes long).';
