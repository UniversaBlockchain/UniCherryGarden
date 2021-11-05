package com.myodov.unicherrygarden.impl.types;

import com.myodov.unicherrygarden.api.types.PrivateKey;
import org.bouncycastle.util.encoders.Hex;
import org.junit.Test;

import java.util.Arrays;

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
    public void testTryWithResourcesSafety() {
        {  // private key 1
            final PrivateKey pkCopy;
            final String addressCopy;
            final byte[] bytesCopy;
            final String bytesHexCopy;

            // Using via try-with-resources
            try (final PrivateKey pk = new PrivateKeyImpl(PK1)) {
                assertEquals(
                        "PrivateKeyImpl<0x34e1e4f805fcdc936068a760b2c17bc62135b5ae>",
                        pk.toString()
                );

                assertEquals(
                        PK1_ADDR.toLowerCase(),
                        pk.getAddress()
                );

                assertEquals(
                        PK1_ADDR,
                        pk.getAddressEip55()
                );

                assertArrayEquals(
                        PK1.clone(),
                        pk.getBytes()
                );

                assertEquals(
                        PK1_STR,
                        pk.getBytesHex()
                );

                // the object leaks outside of try-with-resources zone;
                // for now it still has the private key inside. Will the private key survive leaving
                // the try-with-resources zone?
                pkCopy = pk;
                addressCopy = pk.getAddress();
                bytesCopy = pk.getBytes().clone();
                bytesHexCopy = pk.getBytesHex();

                assertEquals("Still equal: toString",
                        "PrivateKeyImpl<0x34e1e4f805fcdc936068a760b2c17bc62135b5ae>",
                        pkCopy.toString()
                );

                assertEquals("Still equal: getAddress",
                        addressCopy,
                        pkCopy.getAddress()
                );
                assertEquals("Still equal: getAddress",
                        "0x34e1E4F805fCdC936068A760b2C17BC62135b5AE".toLowerCase(),
                        pkCopy.getAddress()
                );

                assertEquals("Still equal: getAddressEip55",
                        PK1_ADDR,
                        pkCopy.getAddressEip55()
                );

                assertArrayEquals("Still equal: getBytes",
                        PK1.clone(),
                        pkCopy.getBytes()
                );
                assertArrayEquals("Still equal: getBytes",
                        bytesCopy,
                        pkCopy.getBytes()
                );

                assertEquals("Still equal: getBytesHex",
                        PK1_STR,
                        pkCopy.getBytesHex()
                );
                assertEquals("Still equal: getBytesHex",
                        bytesHexCopy,
                        pkCopy.getBytesHex()
                );
            }

            // We are leaving the try-with-resources zone so all the private key contents is no more valid

            assertNotEquals("Private key contents is wiped on exiting: toString",
                    "PrivateKeyImpl<0x34e1e4f805fcdc936068a760b2c17bc62135b5ae>",
                    pkCopy.toString()
            );

            assertNotEquals("Private key contents is wiped on exiting: getAddress",
                    PK1_ADDR.toLowerCase(),
                    pkCopy.getAddress()
            );
            assertNotEquals("Private key contents is wiped on exiting: getAddress",
                    addressCopy,
                    pkCopy.getAddress()
            );

            assertNotEquals("Private key contents is wiped on exiting: getAddressEip55",
                    PK1_ADDR,
                    pkCopy.getAddressEip55()
            );

            assertFalse("Private key contents is wiped on exiting: getBytes",
                    Arrays.equals(PK1.clone(), pkCopy.getBytes())
            );
            assertFalse("Private key contents is wiped on exiting: getBytes",
                    Arrays.equals(bytesCopy, pkCopy.getBytes())
            );

            assertNotEquals("Private key contents is wiped on exiting: getBytesHex",
                    PK1_STR,
                    pkCopy.getBytesHex()
            );
            assertNotEquals("Private key contents is wiped on exiting: getBytesHex",
                    bytesHexCopy,
                    pkCopy.getBytesHex()
            );
        }

        {  // private key 2
            final PrivateKey pkCopy;
            final String addressCopy;
            final byte[] bytesCopy;
            final String bytesHexCopy;

            try (final PrivateKeyImpl pk = new PrivateKeyImpl(PK2)) {
                assertEquals(
                        "PrivateKeyImpl<0x408a4ac0e80ba57210ea6a9ae6a9a7b687a51023>",
                        pk.toString()
                );

                assertEquals(
                        PK2_ADDR.toLowerCase(),
                        pk.getAddress()
                );

                assertEquals(
                        PK2_ADDR,
                        pk.getAddressEip55()
                );

                assertArrayEquals(
                        PK2.clone(),
                        pk.getBytes()
                );

                assertEquals(
                        PK2_STR,
                        pk.getBytesHex()
                );

                // the object leaks outside of try-with-resources zone;
                // for now it still has the private key inside. Will the private key survive leaving
                // the try-with-resources zone?
                pkCopy = pk;
                addressCopy = pk.getAddress();
                bytesCopy = pk.getBytes().clone();
                bytesHexCopy = pk.getBytesHex();

                assertEquals("Still equal: toString",
                        "PrivateKeyImpl<0x408a4ac0e80ba57210ea6a9ae6a9a7b687a51023>",
                        pkCopy.toString()
                );

                assertEquals("Still equal: getAddress",
                        addressCopy,
                        pkCopy.getAddress()
                );
                assertEquals("Still equal: getAddress",
                        "0x408a4ac0e80ba57210ea6a9ae6a9a7b687a51023".toLowerCase(),
                        pkCopy.getAddress()
                );

                assertEquals("Still equal: getAddressEip55",
                        PK2_ADDR,
                        pkCopy.getAddressEip55()
                );

                assertArrayEquals("Still equal: getBytes",
                        PK2.clone(),
                        pkCopy.getBytes()
                );
                assertArrayEquals("Still equal: getBytes",
                        bytesCopy,
                        pkCopy.getBytes()
                );

                assertEquals("Still equal: getBytesHex",
                        PK2_STR,
                        pkCopy.getBytesHex()
                );
                assertEquals("Still equal: getBytesHex",
                        bytesHexCopy,
                        pkCopy.getBytesHex()
                );
            }

            // We are leaving the try-with-resources zone so all the private key contents is no more valid

            assertNotEquals("Private key contents is wiped on exiting: toString",
                    "PrivateKeyImpl<0x408a4ac0e80ba57210ea6a9ae6a9a7b687a51023>",
                    pkCopy.toString()
            );

            assertNotEquals("Private key contents is wiped on exiting: getAddress",
                    PK2_ADDR.toLowerCase(),
                    pkCopy.getAddress()
            );
            assertNotEquals("Private key contents is wiped on exiting: getAddress",
                    addressCopy,
                    pkCopy.getAddress()
            );

            assertNotEquals("Private key contents is wiped on exiting: getAddressEip55",
                    PK2_ADDR,
                    pkCopy.getAddressEip55()
            );

            assertFalse("Private key contents is wiped on exiting: getBytes",
                    Arrays.equals(PK2.clone(), pkCopy.getBytes())
            );
            assertFalse("Private key contents is wiped on exiting: getBytes",
                    Arrays.equals(bytesCopy, pkCopy.getBytes())
            );

            assertNotEquals("Private key contents is wiped on exiting: getBytesHex",
                    PK2_STR,
                    pkCopy.getBytesHex()
            );
            assertNotEquals("Private key contents is wiped on exiting: getBytesHex",
                    bytesHexCopy,
                    pkCopy.getBytesHex()
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
