package com.myodov.unicherrygarden.api.types.dlt;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.myodov.unicherrygarden.ethereum.EthUtils;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.time.Instant;
import java.util.Objects;

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
     * Block hash.
     */
    @NonNull
    public final String hash;

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
                 @NonNull String hash,
                 @NonNull Instant ts) {
        assert blockNumber >= 0 : blockNumber;
        assert hash != null && EthUtils.Hashes.isValidBlockHash(hash) : hash;
        assert ts != null : ts;
        this.blockNumber = blockNumber;
        this.hash = hash;
        this.ts = ts;
    }

    @Override
    public String toString() {
        return String.format("%s(%s: %s at %s)",
                getClass().getSimpleName(),
                blockNumber, hash, ts);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        } else if (o == null || getClass() != o.getClass()) {
            return false;
        } else {
            final Block other = (Block) o;
            return (this.blockNumber == other.blockNumber) &&
                    Objects.equals(this.hash, other.hash) &&
                    Objects.equals(this.ts, other.ts);
        }
    }

    @Override
    public int hashCode() {
        return Objects.hash(blockNumber, hash, ts);
    }
}
