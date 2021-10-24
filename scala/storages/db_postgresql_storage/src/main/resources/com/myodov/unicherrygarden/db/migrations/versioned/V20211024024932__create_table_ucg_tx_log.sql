CREATE TABLE ucg_tx_log
(
    id             BIGINT   NOT NULL GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    transaction_id BIGINT   NOT NULL REFERENCES ucg_transaction,
    block_number   INTEGER  NOT NULL REFERENCES ucg_block,
    log_index      SMALLINT NOT NULL
        CHECK (log_index >= 0),
    topics         BYTEA[]  NOT NULL
        CONSTRAINT "all topics are valid" CHECK (ucg_all_topics_are_valid(topics)),
    data           BYTEA    NOT NULL,
    CONSTRAINT "log index unique in block"
        UNIQUE (block_number, log_index)
);

CREATE INDEX IF NOT EXISTS ucg_tx_log_block_number_transaction_id
    ON ucg_tx_log (transaction_id, block_number);

CREATE INDEX IF NOT EXISTS ucg_tx_log_block_number
    ON ucg_tx_log (block_number);
