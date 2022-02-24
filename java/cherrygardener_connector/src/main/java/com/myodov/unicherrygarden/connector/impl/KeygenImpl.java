package com.myodov.unicherrygarden.connector.impl;

import com.myodov.unicherrygarden.api.types.UniCherryGardenError;
import com.myodov.unicherrygarden.connector.api.Keygen;
import com.myodov.unicherrygarden.api.types.PrivateKey;
import com.myodov.unicherrygarden.impl.types.PrivateKeyImpl;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.web3j.crypto.ECKeyPair;
import org.web3j.crypto.Keys;

import java.security.InvalidAlgorithmParameterException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;

/**
 * The default implementation for {@link Keygen} interface.
 */
public final class KeygenImpl implements Keygen {
    @NonNull
    @Override
    public final PrivateKey generatePrivateKey() {
        return _generatePrivateKey();
    }

    /**
     * A statically-available version of private key generation method.
     */
    @NonNull
    public static PrivateKey _generatePrivateKey() {
        try {
            final ECKeyPair pair = Keys.createEcKeyPair();
            return new PrivateKeyImpl(pair);
        } catch (NoSuchAlgorithmException|NoSuchProviderException|InvalidAlgorithmParameterException e) {
            throw new UniCherryGardenError(e.toString());
        }
    }

    @Override
    @NonNull
    public PrivateKey loadPrivateKey(byte[] bytes) {
        return new PrivateKeyImpl(bytes);
    }
}
