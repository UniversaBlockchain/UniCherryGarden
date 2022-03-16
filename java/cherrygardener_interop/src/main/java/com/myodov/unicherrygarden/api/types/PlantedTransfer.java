package com.myodov.unicherrygarden.api.types;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import org.bouncycastle.util.encoders.Hex;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.web3j.crypto.Hash;
import org.web3j.utils.Numeric;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Objects;

/**
 * The class containing the information about the specific currency/token transfer,
 * that is being planted into the Ethereum blockchain by CherryPlanter.
 */
public final class PlantedTransfer extends Transfer {
    private final byte[] bytes;

    /**
     * Constructor.
     */
    @JsonCreator
    public PlantedTransfer(@NonNull String from,
                           @NonNull String to,
                           @NonNull String currencyKey,
                           @NonNull BigDecimal amount,
                           byte[] bytes
    ) {
        super(from, to, currencyKey, amount);
        assert bytes != null : bytes;

        this.bytes = bytes.clone();
    }

    @Override
    public String toString() {
        return String.format("%s(%s %s from %s to %s, hash %s: %s)",
                getClass().getSimpleName(),
                amount, currencyKey, from, to,
                Hex.toHexString(bytes));
    }


    public byte[] getBytes() {
        return bytes.clone();
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
            PlantedTransfer that = (PlantedTransfer) o;
            return Arrays.equals(bytes, that.bytes);
        }
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), Arrays.hashCode(bytes));
    }

    @JsonIgnore
    @NonNull
    public String getHash() {
        return Numeric.toHexString(Hash.sha3(bytes));
    }
}
