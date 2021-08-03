package com.myodov.unicherrygarden.cherrygardener.connector.impl;

import com.myodov.unicherrygarden.cherrygardener.connector.api.Keygen;
import com.myodov.unicherrygarden.cherrygardener.connector.api.types.PrivateKey;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.web3j.crypto.ECKeyPair;
import org.web3j.crypto.Keys;

import java.security.InvalidAlgorithmParameterException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;

public class KeygenImpl implements Keygen {
    /**
     * Keygen to make a proper Ethereum-compatible private key.
     */
    @NonNull
    @Override
    public final PrivateKey _generatePrivateKey() {
        return generatePrivateKey();
    }

    @NonNull
    public static PrivateKey generatePrivateKey() {
        try {
            final ECKeyPair pair = Keys.createEcKeyPair();
            return new PrivateKeyImpl(pair);
        } catch (NoSuchAlgorithmException|NoSuchProviderException|InvalidAlgorithmParameterException e) {
            throw new RuntimeException(e);
        }
    }
}