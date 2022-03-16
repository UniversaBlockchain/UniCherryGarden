package com.myodov.unicherrygarden.api.types.planted.transactions;

import org.checkerframework.checker.nullness.qual.Nullable;

import java.math.BigInteger;

/**
 * The transaction that has been sent to the Ethereum blockchain, or at least attempted.
 */
public class PlantedOutgoingTransaction extends SignedOutgoingTransaction {
    /**
     * Primary constructor (from byte array).
     *
     * @param bytes
     */
    public PlantedOutgoingTransaction(byte[] bytes) {
        super(bytes);
    }

    /**
     * Get the number of confirmation for the transaction.
     *
     * @return The number of confirmations,
     * or <code>null</code> if the transaction hasn’t been successfully registered in the blockchain yet.
     */
    @Nullable
    public BigInteger getNumberOfConfirmations() {
        // TODO
        throw new Error("getNumberOfConfirmations: not implemented yet!");
    }

    /**
     * Get the blockchain block (number) in which the transaction has been registered.
     *
     * @return The blockchain block (number) in which the transaction has been registered,
     * or <code>null</code> if the transaction hasn’t been successfully registered in the blockchain yet.
     */
    @Nullable
    public BigInteger getBlockNumber() {
        // TODO
        throw new Error("getNumberOfConfirmations: not implemented yet!");
    }
}
