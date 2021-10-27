package com.myodov.unicherrygarden.ethereum;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.web3j.crypto.Keys;

import java.math.BigDecimal;
import java.math.BigInteger;

public class EthUtils {
    static final int BLOCK_HASH_LENGTH = 66;
    static final int TRANSACTION_HASH_LENGTH = 66;
    static final int ADDRESS_HASH_LENGTH = 42;


    /**
     * Check if a string is a valid “hex string”, like ones used to store hashes.
     */
    public static final boolean isValidHexString(@NonNull String str) {
        assert str != null;
        return str.startsWith("0x") && str.matches("^0x[0-9a-f]+$");
    }

    /**
     * Check if a `hash` value is valid for usage in Ethereum.
     */
    public static final boolean isValidHash(@NonNull String str, @NonNull int length) {
        assert str != null;
        assert length > 2 : length;
        return (str != null) && (str.length() == length) && isValidHexString(str);
    }

    /**
     * Helpers to deal with Ethereum-style addresses
     */
    public static class Addresses {
        /**
         * Whether the argument is a valid Ethereum address
         * (starts from 0x, then goes the hexadecimal number of proper length in any case).
         */
        public static final boolean isValidAddress(@NonNull String address) {
            return isValidHash(address.toLowerCase(), ADDRESS_HASH_LENGTH);
        }

        /**
         * Whether the argument is a valid Ethereum address
         * (starts from 0x, then goes the hexadecimal number of proper length in any case),
         * and is force-lowercased.
         */
        public static final boolean isValidLowercasedAddress(@NonNull String address) {
            return isValidHash(address, ADDRESS_HASH_LENGTH);
        }

        /**
         * Whether the argument is a valid Ethereum transaction hash
         * (including EIP55 checksum).
         */
        public static final boolean isValidEip55Address(@NonNull String address) {
            return (
                    isValidHash(address.toLowerCase(), ADDRESS_HASH_LENGTH)
                            && Keys.toChecksumAddress(address).equals(address)
            );
        }
    }

    /**
     * Helpers to deal with Ethereum-style hashes.
     */
    public static class Hashes {
        /**
         * Whether the argument is a valid Ethereum block hash.
         */
        public static final boolean isValidBlockHash(@NonNull String hash) {
            return isValidHash(hash, BLOCK_HASH_LENGTH);
        }

        /**
         * Whether the argument is a valid Ethereum transaction hash.
         */
        public static final boolean isValidTransactionHash(@NonNull String hash) {
            return isValidHash(hash, TRANSACTION_HASH_LENGTH);
        }
    }

    /**
     * Helpers to deal with Ethereum-style `Uint256`-based storage of decimal values..
     */
    public static class Uint256 {
        /**
         * Convert the value stored in Ethereum-style `Uint256`
         * (assuming we know the `decimals` amount) to proper `BigDecimal`.
         */
        @NonNull
        public static final BigDecimal valueFromUint256(@NonNull BigInteger uint256, int decimals) {
            assert uint256 != null;
            return new BigDecimal(uint256).divide(BigDecimal.TEN.pow(decimals));
        }

        /**
         * Convert the value to Ethereum-style `Uint256`.
         */
        @NonNull
        public static final BigInteger valueToUint256(@NonNull BigDecimal bd, int decimals) {
            assert bd != null;
            return (bd.multiply(BigDecimal.TEN.pow(decimals))).toBigIntegerExact();
        }
    }

    /**
     * Helpers to deal with Wei/Gwei/Ether conversion.
     */
    public static class Wei {
        /**
         * “Decimals” value for analyzing the raw BigInt Ethereum values.
         */
        private static final int DECIMALS = 18;

        static final BigInteger WEI_IN_ETHER = BigInteger.TEN.pow(DECIMALS);
        static final BigInteger GWEI_IN_ETHER = WEI_IN_ETHER.divide(BigInteger.valueOf(1_000_000_000l));

        private static final BigDecimal WEI_IN_ETHER_BD = new BigDecimal(WEI_IN_ETHER);
        private static final BigDecimal GWEI_IN_ETHER_BD = new BigDecimal(GWEI_IN_ETHER);

        /**
         * Convert the amount (defined in Wei) to regular {@link BigDecimal} value of Ethers.
         */
        @NonNull
        public static final BigDecimal valueFromWeis(@NonNull BigInteger weis) {
            assert weis != null;
            return new BigDecimal(weis).divide(WEI_IN_ETHER_BD);
        }

        /**
         * Convert the amount of Ethers to Wei.
         */
        @NonNull
        public static final BigInteger valueToWeis(@NonNull BigDecimal ethers) {
            assert ethers != null;
            return ethers.multiply(WEI_IN_ETHER_BD).toBigIntegerExact();
        }

        /**
         * Convert the amount (defined in Gwei) to regular {@link BigDecimal} value of Ethers.
         */
        @NonNull
        public static final BigDecimal valueFromGweis(@NonNull BigDecimal gweis) {
            assert gweis != null;
            return gweis.divide(GWEI_IN_ETHER_BD).stripTrailingZeros();
        }

        /**
         * Convert the amount of Ethers to Gwei.
         */
        @NonNull
        public static final BigDecimal valueToGweis(@NonNull BigDecimal ethers) {
            assert ethers != null;
            return ethers.multiply(GWEI_IN_ETHER_BD).stripTrailingZeros();
        }
    }
}
