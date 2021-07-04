package com.myodov.unicherrygarden.cherrygardener.connector.api.types;

import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * Any Ethereum-compatible private key.
 */
public interface PrivateKey {
    @NonNull
    String getAddress();

    @NonNull
    String getAddressEip55();

    /**
     * Binary-safe serialization of the private key.
     *
     * @apiNote always 32 bytes long.
     */
    @NonNull
    byte[] getBytes();

    /**
     * Hex serialization string of the private key.
     *
     * @apiNote always 64 characters long.
     */
    @NonNull
    String getBytesHex();
}
