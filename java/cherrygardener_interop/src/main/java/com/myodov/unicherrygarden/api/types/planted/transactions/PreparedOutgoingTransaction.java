package com.myodov.unicherrygarden.api.types.planted.transactions;

import org.bouncycastle.util.encoders.Hex;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.web3j.crypto.RawTransaction;
import org.web3j.crypto.TransactionDecoder;
import org.web3j.utils.Numeric;

public class PreparedOutgoingTransaction {
    protected final boolean isSigned;

    protected final byte[] bytes;

    /**
     * Primary constructor (from byte array).
     */
    public PreparedOutgoingTransaction(boolean isSigned,
                                       byte[] bytes) {
        assert bytes != null : bytes;

        this.isSigned = isSigned;
        this.bytes = bytes.clone();
    }

    @Override
    public String toString() {
        return String.format("%s(%s, %s)",
                getClass().getSimpleName(),
                isSigned, Hex.toHexString(bytes));
    }

    /**
     * Whether the transaction is signed already.
     */
    public final boolean isSigned() {
        return isSigned;
    }

    /**
     * Get the bytes of transaction, as array of bytes.
     */
    public final byte[] getBytes() {
        return bytes.clone();
    }


    /**
     * Get the bytes of transaction, as a hex string (just hex, not starting from "0x").
     */
    @NonNull
    public final String getBytesHexString() {
        return Hex.toHexString(bytes);
    }

    /**
     * Get the data of transaction, decoded as Web3j {@link RawTransaction} object.
     */
    @NonNull
    public final RawTransaction getRawTransaction() {
        final RawTransaction result = TransactionDecoder.decode(getBytesHexString());
        assert result != null : result;
        return result;
    }

    /**
     * Get an official public representation (hex string, starting with "0x")
     * of the Ethereum transaction.
     */
    @NonNull
    public final String getPublicRepresentation() {
        return Numeric.toHexString(bytes);
    }
}
