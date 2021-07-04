package com.myodov.unicherrygarden.cherrygardener.connector.impl;

import com.myodov.unicherrygarden.cherrygardener.connector.api.types.PrivateKey;
import com.myodov.unicherrygarden.ethereum.Ethereum;
import org.bouncycastle.util.encoders.Hex;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.web3j.crypto.ECKeyPair;
import org.web3j.crypto.Keys;
import org.web3j.utils.Numeric;

public class PrivateKeyImpl implements PrivateKey {
    @NonNull
    private final ECKeyPair keyPair;

    PrivateKeyImpl(@NonNull ECKeyPair keyPair) {
        assert keyPair != null;

        this.keyPair = keyPair;
    }

    PrivateKeyImpl(@NonNull byte[] bytes) {
        this(ECKeyPair.create(bytes));
        assert bytes != null;
        assert bytes.length == Ethereum.PRIVATE_KEY_SIZE_BYTES : bytes.length;
    }

    public String toString() {
        return String.format("PrivateKeyImpl<%s>", getAddress());
    }

    @Override
    public @NonNull String getAddress() {
        return "0x" + Keys.getAddress(keyPair);
    }

    @Override
    public @NonNull String getAddressEip55() {
        return Keys.toChecksumAddress(getAddress());
    }

    /**
     * Binary-safe serialization of the private key.
     *
     * @apiNote always 32 bytes long.
     */
    @NonNull
    @Override
    public byte[] getBytes() {
        return Numeric.toBytesPadded(keyPair.getPrivateKey(), Ethereum.PRIVATE_KEY_SIZE_BYTES);
    }

    /**
     * Hex serialization string of the private key.
     *
     * @apiNote always 64 characters long.
     */
    @Override
    public @NonNull String getBytesHex() {
        final String result = Hex.toHexString(getBytes());
        assert result.length() == Ethereum.PRIVATE_KEY_SIZE_BYTES * 2;
        return result;
    }
}
