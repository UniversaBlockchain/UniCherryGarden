package com.myodov.unicherrygarden.connector.api;

import com.myodov.unicherrygarden.api.types.PrivateKey;
import org.bouncycastle.util.encoders.Hex;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.web3j.crypto.RawTransaction;
import org.web3j.crypto.TransactionDecoder;
import org.web3j.utils.Numeric;

import java.math.BigDecimal;
import java.math.BigInteger;

/**
 * The client connector part that ensures sending of the Ethereum ETH/ERC20 payments.
 */
public interface Sender {

    interface PreparedOutgoingTransaction {
        /**
         * Whether the transaction is signed already.
         */
        boolean isSigned();

        /**
         * Get the bytes of transaction, as array of bytes.
         */
        byte[] getBytes();

        /**
         * Get the data of transaction, decoded as Web3j {@link RawTransaction} object.
         */
        @NonNull
        default RawTransaction getRawTransaction() {
            final RawTransaction result = TransactionDecoder.decode(getBytesHexString());
            assert result != null : result;
            return result;
        }

        /**
         * Get the bytes of transaction, as a hex string (just hex, not starting from "0x").
         */
        @NonNull
        default String getBytesHexString() {
            return Hex.toHexString(getBytes());
        }

        /**
         * Get an official public representation (hex string, starting with "0x")
         * of the Ethereum transaction.
         */
        @NonNull
        default String getPublicRepresentation() {
            return Numeric.toHexString(getBytes());
        }
    }

    interface UnsignedOutgoingTransaction extends PreparedOutgoingTransaction {
        @Override
        default boolean isSigned() {
            return false;
        }

        /**
         * Sign this transaction, using some private key.
         */
        @NonNull
        SignedOutgoingTransaction sign(@NonNull PrivateKey privateKey);
    }

    interface SignedOutgoingTransaction extends PreparedOutgoingTransaction {
        @Override
        default boolean isSigned() {
            return true;
        }

        /**
         * Get tx hash (a string like <code>"0x8fc8b7de7cac3b2ae24ae2d67f35750bccf3d49996313f4d567929e6f6bef44c"</code>)
         * of the Ethereum transaction.
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
         * @return The number of confirmations,
         * or <code>null</code> if the transaction hasn’t been successfully registered in the blockchain yet.
         */
        @Nullable
        BigInteger getNumberOfConfirmations();

        /**
         * Get the blockchain block (number) in which the transaction has been registered.
         *
         * @return The blockchain block (number) in which the transaction has been registered,
         * or <code>null</code> if the transaction hasn’t been successfully registered in the blockchain yet.
         */
        @Nullable
        BigInteger getBlockNumber();
    }

    /**
     * Prepare outgoing transaction without signing it. User should somehow sign it and use it
     * in {@link #sendTransaction}.
     * The signing could be performed either with connector's provided signature function locally or by
     * remote part using some other signing software/backend we'll develop later.
     *
     * @param currencyKey the key of the currency to send. Should be an empty string,
     *                    if sending the primary currency of the blockchain
     *                    (ETH for Ethereum Mainnet, or maybe ETC in case if the backend is used
     *                    for other Ethereum-compatible forks).
     * @param amount      amount to transfer.
     * @return the binary serialized transaction that user can sign on their side,
     * even using the external software (like MyCrypto/MyEtherWallet).
     * @apiNote happens directly in the memory space of the process, without ever leaving it.
     * No network communication is performed. Can be used in the connector launched in “offline” mode.
     */
    @NonNull
    UnsignedOutgoingTransaction buildTransaction(
            @NonNull String receiver,
            @NonNull String currencyKey,
            @NonNull BigDecimal amount
    );

    /**
     * Sign the transaction.
     *
     * @apiNote happens directly in the memory space of the process, without ever leaving it.
     * No network communication is performed. Can be used in the connector launched in “offline” mode.
     */
    @NonNull
    SignedOutgoingTransaction signTransaction(
            @NonNull UnsignedOutgoingTransaction tx,
            byte[] privateKey
    );

    /**
     * Enqueue the transaction for sending (try to send it to the blockchain, etc).
     */
    @NonNull
    SentOutgoingTransaction sendTransaction(@NonNull SignedOutgoingTransaction tx);
}
