package com.myodov.unicherrygarden.cherrygardener.connector.impl;

import com.myodov.unicherrygarden.cherrygardener.connector.api.types.PrivateKey;
import com.myodov.unicherrygarden.ethereum.Ethereum;
import org.bouncycastle.util.encoders.Hex;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.web3j.crypto.ECKeyPair;
import org.web3j.crypto.Keys;
import org.web3j.crypto.Sign;
import org.web3j.utils.Numeric;

import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.util.Arrays;

import static org.web3j.crypto.Sign.getEthereumMessageHash;

/**
 * Default implementation of {@link PrivateKey} interface.
 */
public class PrivateKeyImpl implements PrivateKey {
    @NonNull
    private final byte[] bytes;

    @NonNull
    private final SecureRandom rng = new SecureRandom();


    PrivateKeyImpl(@NonNull byte[] bytes) {
        assert bytes != null;
        assert bytes.length == Ethereum.PRIVATE_KEY_SIZE_BYTES : bytes.length;

        this.bytes = Arrays.copyOf(bytes, bytes.length);
    }

    PrivateKeyImpl(@NonNull ECKeyPair keyPair) {
        this(keyPairToBytes(keyPair));
        assert keyPair != null;
    }

    /**
     * Create a Web3j-compatible {@link ECKeyPair} from <code>byte[]</code> of private key.
     */
    @NonNull
    protected static final ECKeyPair bytesToKeyPair(@NonNull byte[] privateKeyBytes) {
        assert privateKeyBytes != null;
        return ECKeyPair.create(privateKeyBytes);
    }

    /**
     * Make a <code>byte[]</code> representation of private key stored inside a Web3j-compatible {@link ECKeyPair}.
     */
    @NonNull
    protected static final byte[] keyPairToBytes(@NonNull ECKeyPair keyPair) {
        assert keyPair != null;

        return Numeric.toBytesPadded(keyPair.getPrivateKey(), Ethereum.PRIVATE_KEY_SIZE_BYTES);
    }

    @Override
    public void close() {
        rng.nextBytes(bytes);
    }

    @Override
    public String toString() {
        return String.format("PrivateKeyImpl<%s>", getAddress());
    }

    @Override
    public @NonNull String getAddress() {
        return "0x" + Keys.getAddress(Sign.publicKeyFromPrivate(Numeric.toBigInt(bytes)));
    }

    @Override
    public @NonNull String getAddressEip55() {
        return Keys.toChecksumAddress(getAddress());
    }

    @NonNull
    @Override
    public byte[] getBytes() {
        return Arrays.copyOf(bytes, bytes.length);
    }

    @Override
    public @NonNull String getBytesHex() {
        final String result = Hex.toHexString(getBytes());
        assert result.length() == Ethereum.PRIVATE_KEY_SIZE_BYTES * 2;
        return result;
    }

    @Override
    public byte[] signMessage(@NonNull byte[] msg) {
        assert msg != null;

        final ECKeyPair kp = PrivateKeyImpl.bytesToKeyPair(bytes);
        final Sign.SignatureData signatureData =
                Sign.signMessage(getEthereumMessageHash(msg), kp, false);

        final byte[]
                r = signatureData.getR(),
                s = signatureData.getS(),
                v = signatureData.getV();

        return ByteBuffer.allocate(r.length + s.length + v.length)
                .put(r).put(s).put(v)
                .array();
    }
}
