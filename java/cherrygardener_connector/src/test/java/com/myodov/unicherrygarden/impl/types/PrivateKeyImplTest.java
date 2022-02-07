package com.myodov.unicherrygarden.impl.types;

import com.myodov.unicherrygarden.api.types.PrivateKey;
import org.bouncycastle.util.encoders.Hex;
import org.junit.Test;

import java.util.Arrays;

import static com.myodov.unicherrygarden.SampleCredentials.CRED1;
import static com.myodov.unicherrygarden.SampleCredentials.CRED2;
import static org.junit.Assert.*;

public class PrivateKeyImplTest {
    static final String MSG1 = "JohnDoe";
    final String MSG1_SIG = "0xb19e7896f26cbfa692e512950ec75ae6380b0ca5174c959b9561486b8efb5dd45d54b7b723851633b4e428a5a7ede78be90b97d3f6177cf86ff366bb653d64241c";
    final byte[] MSG1_SIG_BYTES = Hex.decode("0xb19e7896f26cbfa692e512950ec75ae6380b0ca5174c959b9561486b8efb5dd45d54b7b723851633b4e428a5a7ede78be90b97d3f6177cf86ff366bb653d64241c".substring(2));


    @Test
    public void testStaticMethods() {
        assertArrayEquals(
                CRED1.bytes.clone(),
                PrivateKeyImpl.keyPairToBytes(PrivateKeyImpl.bytesToKeyPair(CRED1.bytes))
        );

        assertArrayEquals(
                CRED2.bytes.clone(),
                PrivateKeyImpl.keyPairToBytes(PrivateKeyImpl.bytesToKeyPair(CRED2.bytes))
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
            try (final PrivateKey pk = new PrivateKeyImpl(CRED1.bytes)) {
                assertEquals(
                        "PrivateKeyImpl<0x34e1e4f805fcdc936068a760b2c17bc62135b5ae>",
                        pk.toString()
                );

                assertEquals(
                        CRED1.addr.toLowerCase(),
                        pk.getAddress()
                );

                assertEquals(
                        CRED1.addr,
                        pk.getAddressEip55()
                );

                assertArrayEquals(
                        CRED1.bytes.clone(),
                        pk.getBytes()
                );

                assertEquals(
                        CRED1.pk,
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
                        CRED1.addr,
                        pkCopy.getAddressEip55()
                );

                assertArrayEquals("Still equal: getBytes",
                        CRED1.bytes.clone(),
                        pkCopy.getBytes()
                );
                assertArrayEquals("Still equal: getBytes",
                        bytesCopy,
                        pkCopy.getBytes()
                );

                assertEquals("Still equal: getBytesHex",
                        CRED1.pk,
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
                    CRED1.addr.toLowerCase(),
                    pkCopy.getAddress()
            );
            assertNotEquals("Private key contents is wiped on exiting: getAddress",
                    addressCopy,
                    pkCopy.getAddress()
            );

            assertNotEquals("Private key contents is wiped on exiting: getAddressEip55",
                    CRED1.addr,
                    pkCopy.getAddressEip55()
            );

            assertFalse("Private key contents is wiped on exiting: getBytes",
                    Arrays.equals(CRED1.bytes.clone(), pkCopy.getBytes())
            );
            assertFalse("Private key contents is wiped on exiting: getBytes",
                    Arrays.equals(bytesCopy, pkCopy.getBytes())
            );

            assertNotEquals("Private key contents is wiped on exiting: getBytesHex",
                    CRED1.pk,
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

            try (final PrivateKeyImpl pk = new PrivateKeyImpl(CRED2.bytes)) {
                assertEquals(
                        "PrivateKeyImpl<0x408a4ac0e80ba57210ea6a9ae6a9a7b687a51023>",
                        pk.toString()
                );

                assertEquals(
                        CRED2.addr.toLowerCase(),
                        pk.getAddress()
                );

                assertEquals(
                        CRED2.addr,
                        pk.getAddressEip55()
                );

                assertArrayEquals(
                        CRED2.bytes.clone(),
                        pk.getBytes()
                );

                assertEquals(
                        CRED2.pk,
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
                        CRED2.addr,
                        pkCopy.getAddressEip55()
                );

                assertArrayEquals("Still equal: getBytes",
                        CRED2.bytes.clone(),
                        pkCopy.getBytes()
                );
                assertArrayEquals("Still equal: getBytes",
                        bytesCopy,
                        pkCopy.getBytes()
                );

                assertEquals("Still equal: getBytesHex",
                        CRED2.pk,
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
                    CRED2.addr.toLowerCase(),
                    pkCopy.getAddress()
            );
            assertNotEquals("Private key contents is wiped on exiting: getAddress",
                    addressCopy,
                    pkCopy.getAddress()
            );

            assertNotEquals("Private key contents is wiped on exiting: getAddressEip55",
                    CRED2.addr,
                    pkCopy.getAddressEip55()
            );

            assertFalse("Private key contents is wiped on exiting: getBytes",
                    Arrays.equals(CRED2.bytes.clone(), pkCopy.getBytes())
            );
            assertFalse("Private key contents is wiped on exiting: getBytes",
                    Arrays.equals(bytesCopy, pkCopy.getBytes())
            );

            assertNotEquals("Private key contents is wiped on exiting: getBytesHex",
                    CRED2.pk,
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
        try (final PrivateKey pk = new PrivateKeyImpl(CRED1.bytes)) {
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
