package com.myodov.unicherrygarden.api.types.dlt;

import com.fasterxml.jackson.annotation.JsonCreator;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Very high-level details about any Ethereum transaction that has been mined.
 */
public class MinedTx extends Tx {
    /**
     * High-level information about the Ethereum block.
     */
    @NonNull
    public final Block block;

    /**
     * Constructor.
     */
    @JsonCreator
    public MinedTx(@NonNull String txhash,
                   @NonNull String from,
                   @Nullable String to,
                   @NonNull Block block) {
        super(txhash, from, to);

        assert block != null : block;
        this.block = block;
    }

    @Override
    public String toString() {
        return String.format("%s(%s, %s, %s in %d)",
                this.getClass().getSimpleName(),
                txhash, from, to, block);
    }
}
