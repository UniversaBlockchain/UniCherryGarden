package com.myodov.unicherrygarden.api.types.planted.transactions;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.web3j.crypto.Hash;
import org.web3j.crypto.SignedRawTransaction;
import org.web3j.crypto.TransactionEncoder;
import org.web3j.utils.Numeric;

public class SignedOutgoingTransaction extends PreparedOutgoingTransaction {
    /**
     * Primary constructor (from byte array).
     */
    @SuppressWarnings("unused")
    public SignedOutgoingTransaction(byte[] bytes) {
        super(true, bytes);
    }

    /**
     * Secondary constructor (from {@link SignedRawTransaction}).
     */
    @SuppressWarnings("unused")
    public SignedOutgoingTransaction(@NonNull SignedRawTransaction signedRawTransaction) {
        this(TransactionEncoder.encode(signedRawTransaction));
    }

    @Override
    public String toString() {
        return String.format("%s(hash=\"%s\", bytes=\"%s\")",
                getClass().getSimpleName(),
                getHash(), getBytesHexString());
    }


    /**
     * Get tx hash (a string like <code>"0x8fc8b7de7cac3b2ae24ae2d67f35750bccf3d49996313f4d567929e6f6bef44c"</code>)
     * of the Ethereum transaction.
     */
    @NonNull
    public final String getHash() {
        return Numeric.toHexString(Hash.sha3(bytes));
    }
}
