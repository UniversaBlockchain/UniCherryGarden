-- parent_hash will be null for the very first tracked block
ALTER TABLE ucg_block
    ALTER COLUMN parent_hash DROP NOT NULL;
