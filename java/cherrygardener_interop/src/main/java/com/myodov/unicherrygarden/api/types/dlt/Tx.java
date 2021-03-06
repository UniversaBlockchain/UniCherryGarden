package com.myodov.unicherrygarden.api.types.dlt;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.myodov.unicherrygarden.ethereum.EthUtils;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Objects;

/**
 * Very high-level details about any Ethereum transaction.
 */
public class Tx {
    /**
     * Transaction hash.
     */
    @NonNull
    public final String txhash;

    /**
     * Sender of the transaction (address).
     */
    @NonNull
    public final String from;

    /**
     * Receiver of the transaction (address). May be <code>null</code>
     * (if this is a transaction creating the smart contract).
     */
    @Nullable
    public final String to;


    /**
     * Constructor.
     */
    @JsonCreator
    public Tx(@NonNull String txhash,
              @NonNull String from,
              @Nullable String to) {
        assert txhash != null && EthUtils.Hashes.isValidTransactionHash(txhash) : txhash;
        this.txhash = txhash;

        assert from != null && EthUtils.Addresses.isValidLowercasedAddress(from) : from;
        this.from = from;

        assert to == null || EthUtils.Addresses.isValidLowercasedAddress(to) : to;
        this.to = to;
    }

    @Override
    public String toString() {
        return String.format("%s(%s, %s, %s)",
                getClass().getSimpleName(),
                txhash, from, to);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        } else if (o == null || getClass() != o.getClass()) {
            return false;
        } else {
            final Tx other = (Tx) o;
            return Objects.equals(this.txhash, other.txhash) &&
                    Objects.equals(this.from, other.from) &&
                    Objects.equals(this.to, other.to);
        }
    }

    @Override
    public int hashCode() {
        return Objects.hash(txhash, from, to);
    }
}
