CREATE OR REPLACE FUNCTION ucp_block_check_parent_trigger_row()
    RETURNS TRIGGER
    LANGUAGE plpgsql
AS
$$
BEGIN
    -- Dummy implementation
    RETURN NEW;
END;
$$;

CREATE TRIGGER ucp_block_check_parent_row
    BEFORE INSERT OR UPDATE
    ON ucp_block
    FOR EACH ROW
EXECUTE PROCEDURE ucp_block_check_parent_trigger_row();
