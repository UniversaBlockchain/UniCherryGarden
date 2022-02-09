package com.myodov.unicherrygarden.connector.impl;

import com.myodov.unicherrygarden.api.types.UniCherryGardenError;
import com.myodov.unicherrygarden.ethereum.EthUtils;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.math.BigInteger;

/**
 * A convenient set of functions to validate various data and inputs.
 */
public class Validators {
    /**
     * Ensure that some argument refers to a valid Ethereum address.
     *
     * @throws RuntimeException if <code>data</code> is not a valid Ethereum address;
     */
    public static void requireValidEthereumAddress(@NonNull String argname, @NonNull String address) {
        assert argname != null : argname;
        assert address != null : address;
        if (!EthUtils.Addresses.isValidAddress(address)) {
            throw new UniCherryGardenError.NotAnEthereumAddressError(address);
        }
    }

    /**
     * Ensure that some argument refers to a valid lowercased Ethereum address.
     *
     * @throws RuntimeException if <code>data</code> is not a valid lowercased Ethereum address;
     */
    public static void requireValidLowercasedEthereumAddress(@NonNull String argname, @NonNull String address) {
        assert argname != null : argname;
        assert address != null : address;
        if (!EthUtils.Addresses.isValidLowercasedAddress(address)) {
            throw new UniCherryGardenError.NotALowercasedEthereumAddressError(address);
        }
    }

    /**
     * Ensure that some argument refers to a valid Currency Code
     * (either empty string for ETH/ETC base currency, or Ethereum address).
     *
     * @throws RuntimeException if <code>data</code> is not a valid Currency Code.
     */
    public static void requireValidCurrencyKey(@NonNull String argname, @NonNull String currencyKey) {
        assert argname != null : argname;
        assert currencyKey != null : currencyKey;
        if (!currencyKey.isEmpty() && !EthUtils.Addresses.isValidLowercasedAddress(currencyKey)) {
            throw new UniCherryGardenError.NotACurrencyKey(currencyKey);
        }
    }

    /**
     * Ensure that some argument refers to a valid block number (integer, 0 or higher).
     *
     * @throws RuntimeException if <code>data</code> is not a valid block number;
     */
    public static void requireValidBlockNumber(@NonNull String argname, int blockNumber) {
        assert argname != null : argname;
        if (blockNumber < 0) {
            throw new UniCherryGardenError.NotAnBlockNumber(BigInteger.valueOf(blockNumber));
        }
    }

    /**
     * According to EIP-2681 (https://eips.ethereum.org/EIPS/eip-2681), each nonce must be
     * between 0 and 2^64-1.
     */
    public static final BigInteger MAX_NONCE = BigInteger.valueOf(2).pow(64).subtract(BigInteger.ONE); // 2^64 - 1

    /**
     * Ensure that some argument refers to a valid nonce value.
     * According to EIP-2681 (https://eips.ethereum.org/EIPS/eip-2681), each nonce must be
     * between 0 and 2^64-1.
     *
     * @throws RuntimeException if <code>data</code> is not a valid block number;
     */
    public static void requireValidNonce(@NonNull String argname, @NonNull BigInteger nonce) {
        assert argname != null : argname;
        assert nonce != null : nonce;
        if (!(nonce.compareTo(BigInteger.ZERO) >= 0 && nonce.compareTo(MAX_NONCE) <= 0)) {
            throw new UniCherryGardenError.NotAnBlockNumber(nonce);
        }
    }
}
