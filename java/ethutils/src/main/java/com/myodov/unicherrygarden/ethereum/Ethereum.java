package com.myodov.unicherrygarden.ethereum;

/**
 * The API class to keep Ethereum-specific invariants and constants.
 */
public class Ethereum {
    /**
     * Standard size of Ethereum private key, in bytes.
     */
    public static final int PRIVATE_KEY_SIZE_BYTES = 32;

    /**
     * Standard size of Ethereum public key, in bytes.
     */
    public static final int PUBLIC_KEY_SIZE_BYTES = 64;

    public static final class ERC20 {
        public static final String TRANSFER_EVENT_SIGNATURE =
                "0xddf252ad1be2c89b69c2b068fc378daa952ba7f163c4a11628f55a4df523b3ef";
    }
}
