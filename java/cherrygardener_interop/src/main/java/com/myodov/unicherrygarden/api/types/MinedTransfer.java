package com.myodov.unicherrygarden.api.types;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.myodov.unicherrygarden.api.types.dlt.MinedTx;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * The class containing the information about the specific currency/token transfer (that has been mined).
 */
public class MinedTransfer extends Transfer {

    /**
     * Transaction, in which this transfer is present.
     */
    @NonNull
    public final MinedTx tx;

    /**
     * The log index (see <code>eth.getTransactionReceipt(TX).logs.....logIndex</code>) of the transfer.
     * Will likely be <code>null</code> for ETH transfers (that happened in a top-level ETH transaction).
     * Will be non-<code>null</code> for token transfers (as they are registered using exactly the log event).
     * TODO: some logic is needed to track the ETH transfers invoked from smart contract, i.e. “internal transactions”;
     * probably using <code>debug_traceTransaction</code>;
     * for example of such an internal transaction
     * see transaction 0x299271d26d65902c092d95600f8c02e439d0ce568619c23e2126fad7b1fb7f8a.
     */
    @Nullable
    public final Integer logIndex;

    /**
     * Constructor.
     */
    @JsonCreator
    public MinedTransfer(@NonNull String from,
                         @NonNull String to,
                         @NonNull String currencyKey,
                         @NonNull BigDecimal amount,
                         @NonNull MinedTx tx,
                         @Nullable Integer logIndex) {
        super(from, to, currencyKey, amount);

        assert tx != null : tx;
        // For non-ETH transfers, logIndex is mandatory
        if (!currencyKey.isEmpty()) {
            assert logIndex != null && logIndex >= 0 : logIndex;
        }
        this.tx = tx;
        this.logIndex = logIndex;
    }

    @Override
    public String toString() {
        return String.format("%s(%s %s from %s to %s in %s; #%d)",
                getClass().getSimpleName(),
                amount, currencyKey, from, to, tx, logIndex);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        } else if (o == null || getClass() != o.getClass()) {
            return false;
        } else if (!super.equals(o)) {
            return false;
        } else {
            final MinedTransfer other = (MinedTransfer) o;
            return Objects.equals(this.tx, other.tx) &&
                    Objects.equals(this.logIndex, other.logIndex);
        }
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), tx, logIndex);
    }
}
