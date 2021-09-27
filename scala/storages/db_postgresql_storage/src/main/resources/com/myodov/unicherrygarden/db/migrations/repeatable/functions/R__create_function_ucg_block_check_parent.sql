CREATE OR REPLACE FUNCTION ucg_block_check_parent_trigger_row()
    RETURNS TRIGGER
    LANGUAGE plpgsql
AS
$$
DECLARE
    parent_block_number ucg_block.number%TYPE;
BEGIN
    IF NEW.parent_hash IS NOT NULL
    THEN
        -- If parent block is defined, its id must be lower by one.
        SELECT number
        FROM ucg_block
        WHERE hash = NEW.parent_hash
        INTO STRICT parent_block_number;

        IF parent_block_number != NEW.number - 1
        THEN
            RAISE EXCEPTION 'For block % (number %), parent hash id %s (number %, but should be %)',
                NEW.hash, new.number,
                NEW.parent_hash, parent_block_number, NEW.id - 1;
        END IF;
    END IF;

    RETURN NEW;
END;
$$;

COMMENT ON FUNCTION ucg_block_check_parent_trigger_row() IS
    '(Trigger function on each row) Ensure that if a block has a parent block, the id incremented by 1.';
