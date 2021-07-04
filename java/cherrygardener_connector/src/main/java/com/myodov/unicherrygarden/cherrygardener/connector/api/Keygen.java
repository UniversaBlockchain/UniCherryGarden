package com.myodov.unicherrygarden.cherrygardener.connector.api;

import com.myodov.unicherrygarden.cherrygardener.connector.api.types.PrivateKey;
import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * The client connector part that ensures sending of the Ethereum ETH/ERC20 payments.
 */
public interface Keygen {
    /**
     * Keygen to make a proper Ethereum-compatible private key.
     *
     * The implementation class will have a similarly-named static method; use it.
     */
    @NonNull
    PrivateKey _generatePrivateKey();
}
