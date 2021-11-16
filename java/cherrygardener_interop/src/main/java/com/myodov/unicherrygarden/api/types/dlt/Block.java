package com.myodov.unicherrygarden.api.types.dlt;

import com.fasterxml.jackson.annotation.JsonCreator;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.time.Instant;

/**
 * Very high-level details about any Ethereum block (that has been mined).
 */
public class Block {
    /**
     * Number of the Ethereum.
     */
    @NonNull
    public final int blockNumber;

    /**
     * Block timestamp (originally it is Unix timestamp).
     */
    @NonNull
    public final Instant ts;

    /**
     * Constructor.
     */
    @JsonCreator
    public Block(int blockNumber,
                 @NonNull Instant ts) {
        assert blockNumber >= 0 : blockNumber;
        assert ts != null : ts;
        this.blockNumber = blockNumber;
        this.ts = ts;
    }

    @Override
    public String toString() {
        return String.format("%s(%s at %s)",
                this.getClass().getSimpleName(),
                blockNumber, ts);
    }
}
