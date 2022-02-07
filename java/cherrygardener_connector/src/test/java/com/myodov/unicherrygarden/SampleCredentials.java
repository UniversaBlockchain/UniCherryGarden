package com.myodov.unicherrygarden;

import com.myodov.unicherrygarden.api.types.PrivateKey;
import com.myodov.unicherrygarden.impl.types.PrivateKeyImpl;
import org.bouncycastle.util.encoders.Hex;
import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * Set of sample credentials used for unit tests.
 */
public class SampleCredentials {

    public static class CredentialsSet {
        /**
         * Private key, in a string form.
         */
        @NonNull
        public final String pk;

        /**
         * Ethereum address
         */
        @NonNull
        public final String addr;

        public final byte[] bytes;

        @NonNull
        public final PrivateKey privateKey;

        /**
         * Constructor.
         *
         * @param pk   Private Key
         * @param addr Ethereum Address
         */
        CredentialsSet(@NonNull String pk,
                       @NonNull String addr) {
            assert pk != null : pk;
            assert addr != null : addr;

            this.pk = pk;
            this.addr = addr;
            this.bytes = Hex.decode(pk);
            this.privateKey = new PrivateKeyImpl(bytes);
        }
    }

    public static final CredentialsSet CRED1 = new CredentialsSet(
            "f6e28eb945e0835f7ac0f0ccdaaf717f5700554e42ed21fc23580450f5243540",
            "0x34e1E4F805fCdC936068A760b2C17BC62135b5AE"
    );

    public static final CredentialsSet CRED2 = new CredentialsSet(
            "42791cef1f565c4fe1e906e22a39174b46196fb9dc3c8ac56709fce193803b14",
            "0x408a4ac0E80BA57210Ea6A9AE6a9A7B687a51023"
    );
}
