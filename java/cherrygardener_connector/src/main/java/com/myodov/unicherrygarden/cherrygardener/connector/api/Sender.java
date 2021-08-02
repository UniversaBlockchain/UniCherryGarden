package com.myodov.unicherrygarden.cherrygardener.connector.api;

import com.myodov.unicherrygarden.cherrygardener.connector.api.types.Currency;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Optional;

/**
 * The client connector part that ensures sending of the Ethereum ETH/ERC20 payments.
 */
public interface Sender {

    interface PreparedOutgoingTransaction {
        boolean isSigned();
    }

    interface UnsignedOutgoingTransaction extends PreparedOutgoingTransaction {
        @Override
        default boolean isSigned() {
            return false;
        }
    }

    interface SignedOutgoingTransaction extends PreparedOutgoingTransaction {
        @Override
        default boolean isSigned() {
            return true;
        }

        /**
         * Get tx hash (a string like "0x8fc8b7de7cac3b2ae24ae2d67f35750bccf3d49996313f4d567929e6f6bef44c") of the Ethereum transaction.
         */
        @NonNull
        String getHash();
    }

    /**
     * The transaction that has been sent to the Ethereum blockchain, or at least attempted.
     */
    interface SentOutgoingTransaction extends SignedOutgoingTransaction {
        /**
         * Get the number of confirmation for the transaction.
         *
         * @apiNote If <code>Optional</code> is <code>EMPTY</code>,
         * it means that the transaction hasn’t been successfully registered in the blockchain yet.
         */
        @NonNull
        Optional<BigInteger> getNumberOfConfirmations();

        /**
         * Get the blockchain block (number) in which the transaction has been registered.
         *
         * @apiNote If <code>Optional</code> is <code>EMPTY</code>,
         * it means that the transaction hasn’t been successfully registered in the blockchain yet.
         */
        @NonNull
        Optional<BigInteger> getBlockNumber();
    }

    /**
     * Prepare outgoing transaction without signing it. Used should somehow sign it and call performTransaction()
     * The signing could be performed either with connector's provided signature function locally or by
     * remote part using some other signing software we'll develop later
     *
     * @param amount
     * @return packed transaction that user can sign on its side
     */
    @NonNull
    UnsignedOutgoingTransaction buildTransaction(
            @NonNull Currency currency,
            @NonNull BigDecimal amount
    );

    /**
     * Sign the transaction.
     */
    @NonNull
    SignedOutgoingTransaction signTransaction(
            @NonNull UnsignedOutgoingTransaction tx,
            @NonNull byte[] privateKey
    );

    /**
     * Enqueue the transaction for sending (try to send it to the blockchain, etc).
     */
    @NonNull
    SentOutgoingTransaction sendTransaction(@NonNull SignedOutgoingTransaction tx);
}
