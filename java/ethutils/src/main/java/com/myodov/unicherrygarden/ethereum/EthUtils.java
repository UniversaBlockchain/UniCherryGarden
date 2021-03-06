package com.myodov.unicherrygarden.ethereum;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.web3j.crypto.Keys;
import org.web3j.utils.Numeric;

import java.math.BigDecimal;
import java.math.BigInteger;

public class EthUtils {
    static final int BLOCK_HASH_LENGTH = 66;
    static final int TRANSACTION_HASH_LENGTH = 66;
    static final int ADDRESS_HASH_LENGTH = 42;

    public static int BYZANTIUM_FIRST_BLOCK = 4_370_000;

    public static int ETH_TRANSFER_GAS_LIMIT = 21_000;
    public static BigInteger ETH_TRANSFER_GAS_LIMIT_BIGINTEGER = BigInteger.valueOf(ETH_TRANSFER_GAS_LIMIT);


    /**
     * Check if a string is a valid “hex string”, like ones used to store hashes.
     */
    public static boolean isValidHexString(@NonNull String str) {
        if (str == null) {
            throw new IllegalArgumentException("str is null!");
        }
        return str.startsWith("0x") && str.matches("^0x[0-9a-f]+$");
    }

    /**
     * Check if a `str` value is valid for usage in Ethereum as a hex string.
     */
    public static boolean isValidHexString(@NonNull String str, int length) {
        if (str == null) {
            throw new IllegalArgumentException("str is null!");
        }
        if (length <= 2) {
            throw new IllegalArgumentException("length should be >= 2!");
        }
        return (str != null) && (str.length() == length) && isValidHexString(str);
    }

    /**
     * Helpers to deal with Ethereum-style addresses
     */
    public static final class Addresses {
        /**
         * Whether the argument is a valid Ethereum address
         * (starts from 0x, then goes the hexadecimal number of proper length in any case).
         */
        public static boolean isValidAddress(@NonNull String address) {
            if (address == null) {
                throw new IllegalArgumentException("address is null!");
            }
            return isValidHexString(address.toLowerCase(), ADDRESS_HASH_LENGTH);
        }

        /**
         * Whether the argument is a valid Ethereum address
         * (starts from 0x, then goes the hexadecimal number of proper length in any case),
         * and is force-lowercased.
         */
        public static boolean isValidLowercasedAddress(@NonNull String address) {
            return isValidHexString(address, ADDRESS_HASH_LENGTH);
        }

        /**
         * Whether the argument is a valid Ethereum transaction hash
         * (including EIP55 checksum).
         */
        public static boolean isValidEip55Address(@NonNull String address) {
            if (address == null) {
                throw new IllegalArgumentException("address is null!");
            }
            return (
                    isValidHexString(address.toLowerCase(), ADDRESS_HASH_LENGTH)
                            && Keys.toChecksumAddress(address).equals(address)
            );
        }
    }

    /**
     * Helpers to deal with Ethereum-style hashes.
     */
    public static final class Hashes {
        /**
         * Whether the argument is a valid Ethereum block hash.
         */
        public static boolean isValidBlockHash(@NonNull String hash) {
            return isValidHexString(hash, BLOCK_HASH_LENGTH);
        }

        /**
         * Whether the argument is a valid Ethereum transaction hash.
         */
        public static boolean isValidTransactionHash(@NonNull String hash) {
            return isValidHexString(hash, TRANSACTION_HASH_LENGTH);
        }
    }

    /**
     * Helpers to deal with Ethereum-style `Uint256` data, when it is written as a text string
     * (e.g. in Ethereum node RPC responses).
     */
    public static final class Uint256Str {
        /**
         * Convert the value stored in Ethereum-style `Uint256` string
         * to a regular address string.
         *
         * @return A lowercased hex address string (like "0xd701edf8f9c5d834bcb9add73ddeff2d6b9c3d24").
         */
        @NonNull
        public static String toAddress(@NonNull String uint256Str) {
            if (!isValidHexString(uint256Str, 66)) {
                throw new IllegalArgumentException(String.format("%s is not a valid uint256 string!", uint256Str));
            }
            final String emptyPart = uint256Str.substring(2, 26);
            final String contentsPart = uint256Str.substring(26);
            if (!emptyPart.equals("000000000000000000000000")) {
                throw new IllegalArgumentException(String.format("%s is not a valid address±", uint256Str));
            } else {
                return "0x" + contentsPart;
            }
        }

        /**
         * Convert the regular address string to a Ethereum-style `Uint256` string.
         *
         * @param addr Hex address string, e.g. "0xd701edf8f9c5d834bcb9add73ddeff2d6b9c3d24".
         *             Must be lowercased and strictly 42 symbols long.
         */
        @NonNull
        public static String fromAddress(@NonNull String addr) {
            if (!Addresses.isValidLowercasedAddress(addr)) {
                throw new IllegalArgumentException(String.format("%s is not a valid Ethereum address!", addr));
            }
            final String contentsPart = addr.substring(2);
            return "0x000000000000000000000000" + contentsPart;
        }

        /**
         * Convert the value stored in Ethereum-style `Uint256`
         * (assuming we know the `decimals` amount) to proper `BigDecimal`.
         */
        @NonNull
        public static BigInteger toBigInteger(@NonNull String uint256Str) {
            if (!isValidHexString(uint256Str, 66)) {
                throw new IllegalArgumentException(String.format("%s is not a valid uint256 string!", uint256Str));
            }
            return Numeric.toBigInt(uint256Str);
        }
    }

    /**
     * Helpers to deal with Ethereum-style `Uint256`-based storage of decimal values.
     */
    public static final class Uint256 {
        /**
         * Convert the value stored in Ethereum-style `Uint256`
         * (assuming we know the `decimals` amount) to proper `BigDecimal`.
         */
        @NonNull
        public static BigDecimal valueFromUint256(@NonNull BigInteger uint256, int decimals) {
            if (uint256 == null) {
                throw new IllegalArgumentException("uint256 is null!");
            }
            return new BigDecimal(uint256).divide(BigDecimal.TEN.pow(decimals));
        }

        /**
         * Convert the value to Ethereum-style `Uint256`.
         */
        @NonNull
        public static BigInteger valueToUint256(@NonNull BigDecimal bd, int decimals) {
            if (bd == null) {
                throw new IllegalArgumentException("bd is null!");
            }
            return (bd.multiply(BigDecimal.TEN.pow(decimals))).toBigIntegerExact();
        }
    }

    /**
     * Helpers to deal with Wei/Gwei/Ether conversion.
     */
    public static final class Wei {
        /**
         * “Decimals” value for analyzing the raw BigInt Ethereum values.
         */
        private static final int DECIMALS = 18;

        static final BigInteger WEI_IN_ETHER = BigInteger.TEN.pow(DECIMALS);
        static final BigInteger GWEI_IN_ETHER = WEI_IN_ETHER.divide(BigInteger.valueOf(1_000_000_000L));

        private static final BigDecimal WEI_IN_ETHER_BD = new BigDecimal(WEI_IN_ETHER);
        private static final BigDecimal GWEI_IN_ETHER_BD = new BigDecimal(GWEI_IN_ETHER);

        /**
         * Convert the amount (defined in Wei) to regular {@link BigDecimal} value of Ethers.
         */
        @NonNull
        public static BigDecimal valueFromWeis(@NonNull BigInteger weis) {
            if (weis == null) {
                throw new IllegalArgumentException("weis is null!");
            }
            return new BigDecimal(weis).divide(WEI_IN_ETHER_BD);
        }

        /**
         * Convert the amount of Ethers to Wei.
         */
        @NonNull
        public static BigInteger valueToWeis(@NonNull BigDecimal ethers) {
            if (ethers == null) {
                throw new IllegalArgumentException("ethers is null!");
            }
            return ethers.multiply(WEI_IN_ETHER_BD).toBigIntegerExact();
        }

        /**
         * Convert the amount (defined in Gwei) to regular {@link BigDecimal} value of Ethers.
         */
        @NonNull
        public static BigDecimal valueFromGweis(@NonNull BigDecimal gweis) {
            if (gweis == null) {
                throw new IllegalArgumentException("gweis is null!");
            }
            return gweis.divide(GWEI_IN_ETHER_BD).stripTrailingZeros();
        }

        /**
         * Convert the amount of Ethers to Gwei.
         */
        @NonNull
        public static BigDecimal valueToGweis(@NonNull BigDecimal ethers) {
            if (ethers == null) {
                throw new IllegalArgumentException("ethers is null!");
            }
            return ethers.multiply(GWEI_IN_ETHER_BD).stripTrailingZeros();
        }
    }
}
