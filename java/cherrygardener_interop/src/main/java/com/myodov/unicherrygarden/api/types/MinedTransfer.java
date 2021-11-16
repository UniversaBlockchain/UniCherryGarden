package com.myodov.unicherrygarden.api.types;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.myodov.unicherrygarden.api.types.dlt.MinedTx;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.math.BigDecimal;

/**
 * The class containing the information about the specific currency/token transfer (that has been mined).
 */
public class MinedTransfer extends Transfer {

    @NonNull
    public final MinedTx tx;

    /**
     * Constructor.
     */
    @JsonCreator
    public MinedTransfer(@NonNull MinedTx tx,
                         @NonNull String from,
                         @NonNull String to,
                         @NonNull String currencyKey,
                         @NonNull BigDecimal amount) {
        super(from, to, currencyKey, amount);

        assert tx != null : tx;
        this.tx = tx;
    }

    @Override
    public String toString() {
        return String.format("%s(%s %s from %s to %s in %s)",
                this.getClass().getSimpleName(),
                amount, currencyKey, from, to, tx);
    }
}
