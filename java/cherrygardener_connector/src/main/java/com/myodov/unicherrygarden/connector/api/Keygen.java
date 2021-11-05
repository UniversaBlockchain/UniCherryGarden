package com.myodov.unicherrygarden.connector.api;

import com.myodov.unicherrygarden.api.types.PrivateKey;
import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * The client connector part that ensures sending of the Ethereum ETH/ERC20 payments.
 * <p>
 * Each instance doesn’t require networking connectivity, and may be instanced as a class, directly.
 */
public interface Keygen {
    /**
     * Keygen to make a proper Ethereum-compatible private key.
     * <p>
     * You may want to use it via try-with-resources approach.
     */
    @NonNull
    PrivateKey generatePrivateKey();
}
