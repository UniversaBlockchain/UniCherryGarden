CREATE OR REPLACE FUNCTION ucg_block_check_parent_trigger_row()
    RETURNS TRIGGER
    LANGUAGE plpgsql
AS
$$
DECLARE
    how_many_blocks_without_parents BIGINT;
    parent_block_number             ucg_block.number%TYPE;
BEGIN
    IF NEW.parent_hash IS NULL
    THEN
        -- We are trying to add a block which is marked as having no parent.
        -- We must check that there is no other “no-parents” blocks in the database.
        SELECT count(*)
        FROM ucg_block
        WHERE parent_hash IS NULL
        INTO STRICT how_many_blocks_without_parents;

        IF how_many_blocks_without_parents > 0
        THEN
            RAISE EXCEPTION 'Adding block % (number %) without parent, but there is already % block(s) without parent!',
                NEW.hash, new.number, how_many_blocks_without_parents;
        END IF;

    ELSE -- IF NEW.parent_hash IS NOT NULL
    -- We are trying to add a block which is marked as having a parent.
    -- We should check that this is is indeed a next block after that one.
        SELECT number
        FROM ucg_block
        WHERE hash = NEW.parent_hash
        INTO STRICT parent_block_number;

        IF parent_block_number != NEW.number - 1
        THEN
            RAISE EXCEPTION 'For block % (number %), parent hash is %s (number %)!',
                NEW.hash, new.number,
                NEW.parent_hash, parent_block_number;
        END IF;
    END IF;

    RETURN NEW;
END;
$$;

COMMENT ON FUNCTION ucg_block_check_parent_trigger_row() IS
    '(Trigger function on each row) Ensure that if a block has a parent block, the id incremented by 1.';
