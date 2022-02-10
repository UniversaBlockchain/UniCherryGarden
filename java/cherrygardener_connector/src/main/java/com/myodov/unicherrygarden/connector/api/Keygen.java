package com.myodov.unicherrygarden.connector.api;

import com.myodov.unicherrygarden.api.types.PrivateKey;
import com.myodov.unicherrygarden.ethereum.Ethereum;
import org.bouncycastle.util.encoders.Hex;
import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * The client connector part that ensures sending of the Ethereum ETH/ERC20 payments.
 * <p>
 * Each instance doesn’t require networking connectivity, and may be instanced as a class, directly.
 */
public interface Keygen {
    /**
     * Generate a proper Ethereum-compatible private key, in a secure random way.
     * <p>
     * You may want to use it via try-with-resources approach, as in:
     * <pre>
     * try (final PrivateKey pk = keygen.generatePrivateKey()) {
     *     ... Do something with pk
     * }
     * // At this point the data in pk gets explicitly cleaned and not stored in the RAM anymore.
     * </pre>
     */
    @NonNull
    PrivateKey generatePrivateKey();

    /**
     * Load/recreate an Ethereum-compatible private key from an existing byte array.
     * <p>
     * You may want to use it via try-with-resources approach, as in:
     * <pre>
     * try (final PrivateKey pk = keygen.loadPrivateKey(byteArray)) {
     *     ... Do something with pk
     * }
     * // At this point the data in pk gets explicitly cleaned and not stored in the RAM anymore.
     * </pre>
     *
     * @param bytes array of bytes, exactly 32 bytes long.
     */
    @NonNull
    PrivateKey loadPrivateKey(byte[] bytes);

    /**
     * Load/recreate an Ethereum-compatible private key from an existing hex string.
     * <p>
     * You may want to use it via try-with-resources approach, as in:
     * <pre>
     * try (final PrivateKey pk = keygen.loadPrivateKey(hexString)) {
     *     ... Do something with pk
     * }
     * // At this point the data in pk gets explicitly cleaned and not stored in the RAM anymore.
     * </pre>
     *
     * @param hexString hex string containing a private key (without leading "0x");
     *                  must be exactly 64 symbols long.
     */
    @NonNull
    default PrivateKey loadPrivateKey(@NonNull String hexString) {
        assert hexString != null && hexString.length() == Ethereum.PRIVATE_KEY_SIZE_BYTES * 2 : hexString;
        return loadPrivateKey(Hex.decode(hexString));
    }
}
