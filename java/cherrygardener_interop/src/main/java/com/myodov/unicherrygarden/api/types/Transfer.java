package com.myodov.unicherrygarden.api.types;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.myodov.unicherrygarden.ethereum.EthUtils;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.math.BigDecimal;
import java.util.Objects;

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
                getClass().getSimpleName(),
                amount, currencyKey, from, to);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        } else if (o == null || getClass() != o.getClass()) {
            return false;
        } else {
            final Transfer other = (Transfer) o;
            return Objects.equals(this.from, other.from) &&
                    Objects.equals(this.to, other.to) &&
                    Objects.equals(this.currencyKey, other.currencyKey) &&
                    Objects.equals(this.amount, other.amount);
        }
    }

    @Override
    public int hashCode() {
        return Objects.hash(from, to, currencyKey, amount);
    }
}
