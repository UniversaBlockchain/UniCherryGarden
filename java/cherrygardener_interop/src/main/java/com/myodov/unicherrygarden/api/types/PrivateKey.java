package com.myodov.unicherrygarden.api.types;

import org.bouncycastle.util.encoders.Hex;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.ECKeyPair;

/**
 * Any Ethereum-compatible private key.
 * <p>
 * After using the key, you MUST either call the {@link PrivateKey#close()} method to explicitly wipe
 * all the secure contents; or (preferrably) the class should be used in “<i>try-with-resources</i>” manner.
 */
public interface PrivateKey extends AutoCloseable {
    /**
     * “Closes” the underlying key (wiping the private key data).
     *
     * @implNote to prevent leaking of critical data, the default implementation fills the bytes
     * stored in the key with random data (not just zeroes).
     */
    @Override
    void close();

    /**
     * The public address of the private key, a string like “0x9e3319636e2126e3c0bc9e3134aec5e1508a46c7”,
     * normally lowercased.
     */
    @NonNull
    String getAddress();

    /**
     * The public address of the private key, a string like “0x9e3319636e2126e3c0bc9e3134AEC5e1508A46c7”,
     * in a EIP55 mixed-case encoding to contain a checksum preventing mistypes.
     */
    @NonNull
    String getAddressEip55();

    /**
     * Binary-safe serialization of the private key.
     *
     * @apiNote always 32 bytes long.
     */
    byte[] getBytes();

    /**
     * Hex serialization string of the private key.
     * You can use this string to import the private key into Ethereum clients like MyEtherWallet/MyCrypto.
     *
     * @apiNote always 64 characters long.
     */
    @NonNull
    String getBytesHex();

    /**
     * Get the private key as {@link ECKeyPair} Web3j structure.
     */
    @NonNull
    default ECKeyPair getECKeyPair() {
        return ECKeyPair.create(getBytes());
    }

    /**
     * Get the private key as {@link Credentials} Web3j structure.
     */
    @NonNull
    default Credentials getCredentials() {
        return Credentials.create(getECKeyPair());
    }

    /**
     * Sign a text message (actually a byte array) in a manner compatible with MyEtherWallet/MyCrypto
     * “sign” operation.
     * <p>
     *
     * @return just the signature (as bytes), not a complete JSON-like response.
     */
    byte[] signMessage(byte[] msg);

    /**
     * Sign a text message (a text string) in a manner compatible with MyEtherWallet/MyCrypto
     * “sign” operation.
     * <p>
     *
     * @return just the signature (as bytes), not a complete JSON-like response.
     */
    default byte[] signMessage(@NonNull String msg) {
        return signMessage(msg.getBytes());
    }

    /**
     * Sign a text message (a text string) in a manner compatible with MyEtherWallet/MyCrypto
     * “sign” operation.
     * <p>
     *
     * @return the “sig” string of the desired signature.
     */
    @NonNull
    default String signMessageToSig(@NonNull String msg) {
        return "0x" + Hex.toHexString(signMessage(msg.getBytes()));
    }
}
