ALTER TABLE ucg_transaction
    RENAME COLUMN from_hash TO "from";

ALTER TABLE ucg_transaction
    RENAME COLUMN to_hash TO "to";
