package com.myodov.unicherrygarden.api.types.dlt;

import com.fasterxml.jackson.annotation.JsonCreator;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.math.BigDecimal;
import java.util.Objects;

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
     * The index of a transaction in block (see <code>eth.getTransactionReceipt(TX).transactionIndex</code>).
     */
    public final int transactionIndex;

    @NonNull
    public final BigDecimal fees;

    /**
     * Constructor.
     */
    @JsonCreator
    public MinedTx(@NonNull String txhash,
                   @NonNull String from,
                   @Nullable String to,
                   @NonNull Block block,
                   int transactionIndex,
                   @NonNull BigDecimal fees) {
        super(txhash, from, to);

        assert block != null :
                String.format("%s, %s", txhash, block);
        assert transactionIndex >= 0 :
                String.format("%s, %s", txhash, transactionIndex);
        assert fees != null && fees.compareTo(BigDecimal.ZERO) >= 0 : // fees > BigDecimal.ZERO
                String.format("%s, %s", txhash, fees);

        this.block = block;
        this.transactionIndex = transactionIndex;
        this.fees = fees;
    }

    @Override
    public String toString() {
        return String.format("%s(%s, %s, %s, #%d in %s, fees %s)",
                getClass().getSimpleName(),
                txhash, from, to, transactionIndex, block, fees);
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
            final MinedTx other = (MinedTx) o;
            return transactionIndex == other.transactionIndex &&
                    Objects.equals(block, other.block);
        }
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), block, transactionIndex);
    }
}
