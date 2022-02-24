package com.myodov.unicherrygarden.api.types;

import org.checkerframework.checker.nullness.qual.NonNull;

import java.math.BigInteger;

/**
 * Any Error that should not be handled by the caller, but probably change of code is required.
 */
public class UniCherryGardenError extends Error {

    public UniCherryGardenError(String message) {
        super(message);
    }

    /**
     * One of arguments of the call was wrong.
     */
    public static class ArgumentError extends UniCherryGardenError {
        public ArgumentError(String message) {
            super(message);
        }
    }

    /**
     * One of arguments that should contain a valid Ethereum address, contains wrong data.
     */
    public static class NotAnEthereumAddressError extends ArgumentError {
        public NotAnEthereumAddressError(@NonNull String badValue) {
            super(String.format("%s is not a valid Ethereum address", badValue));
        }
    }

    /**
     * One of arguments that should contain a valid Ethereum address (it is required to be lowercased),
     * contains wrong data.
     */
    public static class NotALowercasedEthereumAddressError extends NotAnEthereumAddressError {
        public NotALowercasedEthereumAddressError(@NonNull String badValue) {
            super(String.format("%s is not a valid lowercased Ethereum address", badValue));
        }
    }

    /**
     * One of arguments that should contain a valid Currency Code (either a valid lowercased Ethereum address,
     * or empty string), contains wrong data.
     */
    public static class NotACurrencyKey extends NotAnEthereumAddressError {
        public NotACurrencyKey(@NonNull String badValue) {
            super(String.format("%s is not a valid Currency Code", badValue));
        }
    }

    /**
     * One of arguments that should contain a valid block number, contains wrong data.
     */
    public static class NotAnBlockNumber extends ArgumentError {
        public NotAnBlockNumber(@NonNull BigInteger badValue) {
            super(String.format("%s is not a valid block number (should be >= 0)", badValue));
        }
    }

    /**
     * One of arguments that should contain a valid nonce, contains wrong data.
     */
    public static class NotAnValidNonce extends ArgumentError {
        public NotAnValidNonce(@NonNull BigInteger badValue) {
            super(String.format("%s is not a valid block number (should be 0 <= nonce <= 2^64-1 per EIP-2681)",
                    badValue));
        }
    }

    /**
     * Some network problem occurred during the operation.
     */
    public static class NetworkError extends UniCherryGardenError {
        public NetworkError(String message) {
            super(message);
        }
    }
}
