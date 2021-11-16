package com.myodov.unicherrygarden.api.types;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.myodov.unicherrygarden.ethereum.EthUtils;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.math.BigDecimal;

/**
 * The class containing the information about the specific currency/token transfer.
 */
public class Transfer {
    @NonNull
    public final String from;

    @NonNull
    public final String to;

    @NonNull
    public final String currencyKey;

    @NonNull
    public final BigDecimal amount;


    /**
     * Constructor.
     */
    @JsonCreator
    public Transfer(@NonNull String from,
                    @NonNull String to,
                    @NonNull String currencyKey,
                    @NonNull BigDecimal amount) {
        assert from != null && EthUtils.Addresses.isValidLowercasedAddress(from) : from;
        assert to != null && EthUtils.Addresses.isValidLowercasedAddress(to) : to;
        assert currencyKey != null : currencyKey;
        assert (amount != null && amount.compareTo(BigDecimal.ZERO) >= 0) : amount; // amount >= 0

        this.from = from;
        this.to = to;
        this.currencyKey = currencyKey;
        this.amount = amount;
    }

    @Override
    public String toString() {
        return String.format("%s(%s %s from %s to %s)",
                this.getClass().getSimpleName(),
                amount, currencyKey, from, to);
    }
}
