package com.myodov.unicherrygarden.impl.types;

import com.myodov.unicherrygarden.api.types.PrivateKey;
import org.bouncycastle.util.encoders.Hex;
import org.junit.Test;

import static org.junit.Assert.*;

public class PrivateKeyImplTest {
    static final String PK1_STR = "f6e28eb945e0835f7ac0f0ccdaaf717f5700554e42ed21fc23580450f5243540";
    static final byte[] PK1 = Hex.decode(PK1_STR);
    static final String PK1_ADDR = "0x34e1E4F805fCdC936068A760b2C17BC62135b5AE";

    static final String PK2_STR = "42791cef1f565c4fe1e906e22a39174b46196fb9dc3c8ac56709fce193803b14";
    static final byte[] PK2 = Hex.decode(PK2_STR);
    static final String PK2_ADDR = "0x408a4ac0E80BA57210Ea6A9AE6a9A7B687a51023";

    static final String MSG1 = "JohnDoe";
    final String MSG1_SIG = "0xb19e7896f26cbfa692e512950ec75ae6380b0ca5174c959b9561486b8efb5dd45d54b7b723851633b4e428a5a7ede78be90b97d3f6177cf86ff366bb653d64241c";
    final byte[] MSG1_SIG_BYTES = Hex.decode("0xb19e7896f26cbfa692e512950ec75ae6380b0ca5174c959b9561486b8efb5dd45d54b7b723851633b4e428a5a7ede78be90b97d3f6177cf86ff366bb653d64241c".substring(2));


    @Test
    public void testStaticMethods() {
        assertArrayEquals(
                PK1.clone(),
                PrivateKeyImpl.keyPairToBytes(PrivateKeyImpl.bytesToKeyPair(PK1))
        );

        assertArrayEquals(
                PK2.clone(),
                PrivateKeyImpl.keyPairToBytes(PrivateKeyImpl.bytesToKeyPair(PK2))
        );
    }

    @Test
    public void testBasics() {
        {  // private key 1
            final PrivateKey pkCopy;

            // Using via try-with-resources
            try (final PrivateKey pk = new PrivateKeyImpl(PK1)) {
                assertEquals(
                        PK1_STR,
                        pk.getBytesHex()
                );

                assertArrayEquals(
                        PK1.clone(),
                        pk.getBytes()
                );

                assertEquals(
                        PK1_ADDR.toLowerCase(),
                        pk.getAddress()
                );

                assertEquals(
                        PK1_ADDR,
                        pk.getAddressEip55()
                );

                assertEquals(
                        "PrivateKeyImpl<0x34e1e4f805fcdc936068a760b2c17bc62135b5ae>",
                        pk.toString()
                );

                // the object leaks outside of try-with-resources zone;
                // for now it still has the private key inside. Will the private key survive leaving
                // the try-with-resources zone?
                pkCopy = pk;

                assertArrayEquals(
                        "Still equal",
                        PK1.clone(),
                        pkCopy.getBytes()
                );

                assertEquals(
                        "Still equal",
                        "PrivateKeyImpl<0x34e1e4f805fcdc936068a760b2c17bc62135b5ae>",
                        pkCopy.toString()
                );
            }
            // We are leaving the try-with-resources zone so all the private key contents is no more valid
            assertNotEquals(
                    "private key contents is wiped on exiting",
                    PK1_STR,
                    pkCopy.getBytesHex()
            );

            assertNotEquals(
                    "private key contents is wiped on exiting",
                    PK1_ADDR.toLowerCase(),
                    pkCopy.getAddress()
            );

            assertNotEquals(
                    "private key contents is wiped on exiting",
                    PK1_ADDR,
                    pkCopy.getAddressEip55()
            );

            assertNotEquals(
                    "private key contents is wiped on exiting",
                    "PrivateKeyImpl<0x34e1e4f805fcdc936068a760b2c17bc62135b5ae>",
                    pkCopy.toString()
            );
        }

        {  // private key 2
            final PrivateKey pkCopy;

            try (final PrivateKeyImpl pk = new PrivateKeyImpl(PK2)) {
                assertEquals(
                        PK2_STR,
                        pk.getBytesHex()
                );

                assertArrayEquals(
                        PK2.clone(),
                        pk.getBytes()
                );

                assertEquals(
                        PK2_ADDR.toLowerCase(),
                        pk.getAddress()
                );

                assertEquals(
                        PK2_ADDR,
                        pk.getAddressEip55()
                );

                assertEquals(
                        "PrivateKeyImpl<0x408a4ac0e80ba57210ea6a9ae6a9a7b687a51023>",
                        pk.toString()
                );

                // the object leaks outside of try-with-resources zone;
                // for now it still has the private key inside. Will the private key survive leaving
                // the try-with-resources zone?
                pkCopy = pk;

                assertArrayEquals(
                        "Still equal",
                        PK2.clone(),
                        pkCopy.getBytes()
                );

                assertEquals(
                        "Still equal",
                        "PrivateKeyImpl<0x408a4ac0e80ba57210ea6a9ae6a9a7b687a51023>",
                        pkCopy.toString()
                );
            }

            // We are leaving the try-with-resources zone so all the private key contents is no more valid
            assertNotEquals(
                    "private key contents is wiped on exiting",
                    PK2_STR,
                    pkCopy.getBytesHex()
            );

            assertNotEquals(
                    "private key contents is wiped on exiting",
                    PK2_ADDR.toLowerCase(),
                    pkCopy.getAddress()
            );

            assertNotEquals(
                    "private key contents is wiped on exiting",
                    PK2_ADDR,
                    pkCopy.getAddressEip55()
            );

            assertNotEquals(
                    "private key contents is wiped on exiting",
                    "PrivateKeyImpl<0x408a4ac0e80ba57210ea6a9ae6a9a7b687a51023>",
                    pkCopy.toString()
            );
        }
    }

    @Test
    public void testSignMessage() {
        try (final PrivateKey pk = new PrivateKeyImpl(PK1)) {
            assertArrayEquals(
                    MSG1_SIG_BYTES,
                    pk.signMessage(MSG1)
            );
            assertArrayEquals(
                    MSG1_SIG_BYTES,
                    pk.signMessage(MSG1.getBytes())
            );
            assertEquals(
                    MSG1_SIG,
                    pk.signMessageToSig(MSG1)
            );
        }
    }
}
